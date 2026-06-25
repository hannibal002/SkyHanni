package at.hannibal2.skyhanni.data.foraging

import at.hannibal2.skyhanni.api.CollectionApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.IslandTypeTag
import at.hannibal2.skyhanni.data.ItemAddManager
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.CollectionUpdateEvent
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.SackChangeEvent
import at.hannibal2.skyhanni.events.foraging.ForagingCollectionAddEvent
import at.hannibal2.skyhanni.features.foraging.CompactSweepDetails
import at.hannibal2.skyhanni.features.foraging.ForagingLogType
import at.hannibal2.skyhanni.features.foraging.ForagingLogType.Companion.getForagingLogType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ForagingCollectionApi {

    private val storage get() = ProfileStorageData.profileSpecific?.foraging

    var lastGainedLog: ForagingLogType?
        get() = storage?.lastGainedLog
        set(value) {
            value?.let { storage?.lastGainedLog = it }
        }

    var lastGainedCollectionTime: SimpleTimeMark
        get() = storage?.lastGainedLogCollectionTime ?: SimpleTimeMark.farPast()
        set(value) {
            storage?.lastGainedLogCollectionTime = value
        }

    // The log type the player is currently chopping, updated on each new block click.
    var lastClickedLogType: ForagingLogType? = null
        private set
    private var lastLogClickTime = SimpleTimeMark.farPast()
    private var lastLogClickPosition: LorenzVec? = null

    // Returns the log type the player is actively chopping (within the last 5s), or null.
    fun getCurrentlyChopping(): ForagingLogType? =
        if (lastLogClickTime.passedSince() < 5.seconds) lastClickedLogType else null

    /**
     * Detects which log block the player is breaking so the display can switch tree types.
     */
    @HandleEvent(onlyOnSkyblock = true)
    fun onBlockClick(event: BlockClickEvent) {
        if (event.clickType != ClickType.LEFT_CLICK) return
        if (!IslandTypeTag.FORAGING.isInIsland() && !IslandType.HUB.isInIsland()) return

        val rawType = event.blockState.getForagingLogType() ?: return

        // In Galatea, stripped-spruce blocks are used for Fig trees (not actual Spruce).
        // Override the block-state detection to return FIG when in that island.
        val logType = if (rawType == ForagingLogType.SPRUCE && IslandType.GALATEA.isInIsland()) {
            ForagingLogType.FIG
        } else rawType

        // Deduplicate: ignore repeated clicks on the same block position
        if (lastLogClickPosition == event.position) return

        lastLogClickPosition = event.position
        lastClickedLogType = logType
        lastLogClickTime = SimpleTimeMark.now()

        // Optimistically count logs per unique block break using the game's logs-per-break figure
        // from the most recent "Sweep Details" message for this tree type. This already accounts
        // for Sweep toughness, Foraging Fortune, and cutting penalties, so it's a much closer
        // estimate than a naive +1. Falls back to 1 before any Sweep Details message has arrived.
        val logsEstimate = (CompactSweepDetails.lastKnownLogsPerBreak[logType] ?: 1.0).toLong().coerceAtLeast(1L)
        optimisticGains[logType] = (optimisticGains[logType] ?: 0L) + logsEstimate

        ChatUtils.debug("ForagingCollectionApi: block-click detected $logType (raw=$rawType)")
    }

    fun isCurrentlyForaging(): Boolean = lastGainedCollectionTime.passedSince() < 5.seconds

    fun ForagingLogType.getCollection(): Long =
        storage?.foragingCollection?.get(this) ?: 0L

    fun ForagingLogType.setCollectionCounter(counter: Long) {
        val previous = storage?.foragingCollection?.get(this) ?: 0L
        if (counter > previous) {
            lastGainedLog = this
            lastGainedCollectionTime = SimpleTimeMark.now()
        }
        storage?.foragingCollection?.set(this, counter)
        val delta = (counter - previous).coerceAtLeast(0L)
        if (delta > 0L) ForagingCollectionAddEvent(this, delta).post()
        ChatUtils.debug("Set $this foraging collection to $counter")
    }

    @HandleEvent
    fun onItemAdd(event: ItemAddEvent) {
        // Only handle direct inventory pickups here; sack changes are handled in onSackChange below.
        // This prevents double-counting when ItemAddManager converts a SackChangeEvent into an ItemAddEvent.
        if (event.source != ItemAddManager.Source.ITEM_ADD) return
        val logType = ForagingLogType.entries.find { it.internalName == event.internalName } ?: return
        val delta = event.amount.toLong()
        if (delta <= 0L) return
        recordLogGain(logType, delta)
        ChatUtils.debug("ItemAdd(inv): incremented $logType foraging collection by $delta")
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSackChange(event: SackChangeEvent) {
        // Handle sack changes directly; this bypasses ItemAddManager's post-sack-inventory guard
        // (which ignores sack events for 10s after closing a sack inventory) and ensures logs
        // going into a Foraging Sack are always tracked.
        for (change in event.sackChanges) {
            val delta = change.delta.toLong()
            if (delta <= 0L) continue
            // Direct match on SkyBlock internal name (e.g. LOG-2 for Birch, MANGROVE_LOG, etc.)
            val logType = ForagingLogType.entries.find { it.internalName == change.internalName }
                // Fallback: strip ENCHANTED_ prefix so ENCHANTED_MANGROVE_LOG maps to MANGROVE_LOG / MANGROVE
                ?: ForagingLogType.entries.find {
                    it.internalName.asString() == change.internalName.asString().removePrefix("ENCHANTED_")
                }
                ?: continue
            recordLogGain(logType, delta)
            ChatUtils.debug("SackChange: incremented $logType foraging collection by $delta")
        }
    }

    /** Updates the stored collection counter and fires [ForagingCollectionAddEvent]. */
    private fun recordLogGain(logType: ForagingLogType, delta: Long) {
        val current = storage?.foragingCollection?.get(logType) ?: 0L
        val newAmount = current + delta
        lastGainedLog = logType
        lastGainedCollectionTime = SimpleTimeMark.now()
        storage?.foragingCollection?.set(logType, newAmount)
        // Reconcile optimistic estimate: subtract the real delta from pending optimistic count.
        val pending = optimisticGains[logType] ?: 0L
        if (pending > 0L) optimisticGains[logType] = maxOf(0L, pending - delta)
        ForagingCollectionAddEvent(logType, delta).post()
    }

    @HandleEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        storage?.foragingCollection?.clear()
        optimisticGains.clear()
    }

    @HandleEvent
    fun onCollectionUpdate(event: CollectionUpdateEvent) {
        for (logType in ForagingLogType.entries) {
            val amount = CollectionApi.collectionValue[logType.internalName] ?: continue
            val current = storage?.foragingCollection?.get(logType) ?: 0L
            if (amount == current) continue
            if (amount > current) {
                lastGainedLog = logType
                lastGainedCollectionTime = SimpleTimeMark.now()
            }
            storage?.foragingCollection?.set(logType, amount)
            val delta = (amount - current).coerceAtLeast(0L)
            if (delta > 0L) ForagingCollectionAddEvent(logType, delta).post()
        }
    }

    /**
     * Optimistic per-type gain accumulated from block-break position changes.
     * Each unique block position click adds the game's observed logs-per-break for that tree type,
     * falling back to 1 before any Sweep Details message has arrived.
     * Stored as Long since you can't break a fraction of a log. No cap is applied.
     * Reconciled down when real ForagingCollectionAddEvents arrive so the display stays accurate.
     */
    val optimisticGains: MutableMap<ForagingLogType, Long> = mutableMapOf()
}
