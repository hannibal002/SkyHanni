package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.TitleManager
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.canBeSeen
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledBoundingBox
import at.hannibal2.skyhanni.utils.RenderUtils.expandBlock
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.RenderableString
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.ItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SkyHanniItemTracker
import com.google.gson.annotations.Expose
import net.minecraft.init.Blocks
import net.minecraft.util.AxisAlignedBB
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object DarkMonolithFeatures {

    class Data : ItemTrackerData() {
        override fun resetItems() {
            monolithsLooted = 0
        }

        override fun getDescription(timesGained: Long) = emptyList<String>()
        override fun getCoinName(item: TrackedItem) = "§5Dark Monolith §6Coins"
        override fun getCoinDescription(item: TrackedItem) = emptyList<String>()

        @Expose
        var monolithsLooted: Long = 0
    }

    private val mithrilPowderItem = "SKYBLOCK_POWDER_MITHRIL".toInternalName()
    private val rockTheFishItem = "ROCK_THE_FISH".toInternalName()

    private val patternGroup = RepoPattern.group("mining.dwarves.darkmonolith")
    private val config get() = SkyHanniMod.feature.mining.darkMonolith
    private val tracker = SkyHanniItemTracker("Dark Monolith Tracker", { Data() }, { it.mining.darkMonolithTracker }) {
        drawDisplay(it)
    }

    // Todo: need chat pattern for rock the fish drop
    /**
     * REGEX-TEST: §5§lMONOLITH! §r§aYou found a mysterious §r§5Dark Monolith §r§aand were rewarded §r§650,000 Coins§r§a!
     * REGEX-TEST: §5§lMONOLITH! §r§aYou found a mysterious §r§5Dark Monolith §r§aand were rewarded §r§62,500 Coins §r§aand §r§21,000 ᠅ Mithril Powder§r§a!
     * REGEX-TEST: §5§lMONOLITH! §r§aYou found a mysterious §r§5Dark Monolith §r§aand were rewarded §r§2100 ᠅ Mithril Powder§r§a!
     */
    @Suppress("MaxLineLength")
    private val dropPattern by patternGroup.pattern(
        "drop",
        "§5§lMONOLITH! §r§aYou.*§r§aand were rewarded ?(?:(?:§.)+(?<coins>[\\d,]+) Coins ?(?:§.)+)?(?:!|and )?(?:(?:§.)+(?<powder>[\\d,]+) ᠅ Mithril Powder§r§a!)?"
    )

    private var knownEggs: Set<LorenzVec> = setOf()
    private var foundEggVec: LorenzVec? = null
    private var lastFoundEggVec: LorenzVec? = null
    private var renderBox: AxisAlignedBB? = null

    private fun reset() {
        knownEggs = setOf()
        foundEggVec = null
        lastFoundEggVec = null
        renderBox = null
    }

    init {
        RenderDisplayHelper(
            outsideInventory = true,
            inOwnInventory = true,
            condition = { config.tracker && IslandType.DWARVEN_MINES.isCurrent() },
            onRender = {
                tracker.renderDisplay(config.trackerPosition)
            },
        )
    }

    @HandleEvent(onlyOnIsland = IslandType.DWARVEN_MINES)
    fun onChat(event: SkyHanniChatEvent) {
        dropPattern.matchMatcher(event.message) {
            reset()
            if (!config.tracker) return
            groupOrNull("coins")?.let {
                tracker.addCoins(it.formatInt(), false)
            }
            groupOrNull("powder")?.let {
                tracker.addItem(mithrilPowderItem, it.formatInt(), false)
            }
            groupOrNull("fish")?.let {
                tracker.addItem(rockTheFishItem, 1, false)
            }
        }
    }

    @HandleEvent
    fun onWorldChange() {
        reset()
    }

    @HandleEvent(onlyOnIsland = IslandType.DWARVEN_MINES)
    fun onSecondPassed() {
        if (!isEnabled()) return
        knownEggs = BlockUtils.nearbyBlocks(
            LocationUtils.playerLocation(),
            distance = 40,
            filter = Blocks.dragon_egg,
        ).keys
    }

    @HandleEvent(onlyOnIsland = IslandType.DWARVEN_MINES)
    fun onTick() {
        if (!isEnabled()) return
        foundEggVec = knownEggs.firstOrNull { it.canBeSeen() }
        checkTitle()
        lastFoundEggVec = foundEggVec
        val knownEggVec = foundEggVec ?: return
        renderBox = knownEggVec.boundingToOffset(1.0, 1.0, 1.0).expandBlock()
    }

    private fun checkTitle() {
        if (lastFoundEggVec != null || foundEggVec == null) return
        val titleText = config.title.takeIf { it.isNotEmpty() } ?: return
        TitleManager.sendTitle(titleText, duration = 3.seconds)
    }

    private fun drawDisplay(data: Data): List<Searchable> = buildList {
        if (!config.tracker) return@buildList
        addSearchString("§5§lDark Monolith Tracker")
        val profit = tracker.drawItems(data, { true }, this)
        add(
            RenderableString(
                "§7Monoliths looted: §d${data.monolithsLooted}"
            ).toSearchable()
        )
        add(tracker.addTotalProfit(profit, data.monolithsLooted, "loot"))
        tracker.addPriceFromButton(this)
    }

    @HandleEvent(onlyOnIsland = IslandType.DWARVEN_MINES)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return
        val knownEggVec = foundEggVec ?: return
        if (knownEggVec.distanceToPlayer() >= 100) return
        val axis = renderBox ?: return
        event.drawFilledBoundingBox(axis, config.highlightColor.toColor())
    }


    private fun isEnabled() = config.highlight || config.title.isNotEmpty()
}
