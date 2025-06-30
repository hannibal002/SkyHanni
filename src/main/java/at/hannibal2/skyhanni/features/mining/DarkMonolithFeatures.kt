package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.mining.dwarves.DarkMonolithConfig
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.canBeSeen
import at.hannibal2.skyhanni.utils.LocationUtils.maxBox
import at.hannibal2.skyhanni.utils.LocationUtils.minBox
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.StringRenderable
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.ItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SkyHanniItemTracker
import com.google.gson.annotations.Expose
import net.minecraft.init.Blocks
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.EnumFacing
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object DarkMonolithFeatures {

    class Data : ItemTrackerData() {
        override fun resetItems() {
            monolithsLooted = 0
        }

        override fun getDescription(timesGained: Long) = emptyList<String>()
        override fun getCoinName(item: TrackedItem) = "§6Monolith Coins"
        override fun getCoinDescription(item: TrackedItem) = emptyList<String>()

        @Expose
        var monolithsLooted: Long = 0
    }

    private val mithrilPowderItem = "SKYBLOCK_POWDER_MITHRIL".toInternalName()
    private val rockTheFishItem = "ROCK_THE_FISH".toInternalName()

    private val patternGroup = RepoPattern.group("mining.dwarves.darkmonolith")
    private val config get() = SkyHanniMod.feature.mining.darkMonolith
    private val tracker = SkyHanniItemTracker(
        "Dark Monolith Tracker",
        createNewSession = { Data() },
        getStorage = { it.mining.darkMonolithTracker }
    ) { drawDisplay(it) }

    // Todo: need chat pattern for rock the fish drop
    /**
     * REGEX-TEST: §5§lMONOLITH! §r§aYou found a mysterious §r§5Dark Monolith §r§aand were rewarded §r§650,000 Coins§r§a!
     * REGEX-TEST: §5§lMONOLITH! §r§aYou found a mysterious §r§5Dark Monolith §r§aand were rewarded §r§62,500 Coins §r§aand §r§21,000 ᠅ Mithril Powder§r§a!
     * REGEX-TEST: §5§lMONOLITH! §r§aYou found a mysterious §r§5Dark Monolith §r§aand were rewarded §r§2100 ᠅ Mithril Powder§r§a!
     * REGEX-TEST: §5§lMONOLITH! §r§aYou found a mysterious §r§5Dark Monolith §r§aand were rewarded §r§23,000 ᠅ Mithril Powder§r§a!
     */
    @Suppress("MaxLineLength")
    private val dropPattern by patternGroup.pattern(
        "drop",
        "§5§lMONOLITH! §r§aYou.*§r§aand were rewarded ?(?:(?:§.)+(?<coins>[\\d,]+) Coins ?(?:§.)+)?(?:!|and )?(?:(?:§.)+(?<powder>[\\d,]+) ᠅ Mithril Powder§r§a!)?",
    )

    private var knownEggs: Set<LorenzVec> = setOf()
    private var foundEggVec: LorenzVec? = null
    private var lastFoundEggVec: LorenzVec? = null
    private var renderBox: AxisAlignedBB? = null
    private var nextBlockCheck: SimpleTimeMark = SimpleTimeMark.farPast()

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
            DarkMonolithFeatures.reset()
            groupOrNull("coins")?.let {
                tracker.addCoins(it.formatInt(), false)
            }
            groupOrNull("powder")?.let {
                tracker.addItem(mithrilPowderItem, it.formatInt(), false)
            }
            groupOrNull("fish")?.let {
                tracker.addItem(rockTheFishItem, 1, false)
            }
            tracker.modify {
                it.monolithsLooted++
            }
        }
    }

    @HandleEvent
    fun onWorldChange(event: IslandChangeEvent) {
        reset()
        if (event.newIsland == IslandType.DWARVEN_MINES) {
            tracker.firstUpdate()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.DWARVEN_MINES)
    fun onSecondPassed() {
        if (!anyEnabled()) return
        knownEggs = BlockUtils.nearbyBlocks(
            LocationUtils.playerLocation(),
            distance = 40,
            filter = Blocks.dragon_egg,
        ).keys
    }

    @HandleEvent(onlyOnIsland = IslandType.DWARVEN_MINES)
    fun onTick() {
        if (!anyEnabled()) return
        val knownEggVec = foundEggVec ?: return
        with(WorldRenderUtils) {
            renderBox = knownEggVec.boundingToOffset(1.0, 1.0, 1.0).expandBlock()
        }
    }

    private fun updateKnownEggs() {
        if (nextBlockCheck.isInFuture()) return
        foundEggVec = knownEggs.firstOrNull { blockVec ->
            val base = blockVec.floor()
            val aabb = base.boundingToOffset(1.0, 1.0, 1.0)
            LocationUtils.canSeeAnyFace(
                min = aabb.minBox(),
                max = aabb.maxBox(),
                stepCount = 4,
                ignoreFaces = listOf(EnumFacing.DOWN).toTypedArray(),
            )
        }.also {
            checkTitle()
            lastFoundEggVec = it
            nextBlockCheck = SimpleTimeMark.now().plus(500.milliseconds)
        }
    }

    private fun checkTitle() {
        if (lastFoundEggVec != null || foundEggVec == null) return
        if (!config.title.enabled) return
        val titleText = config.title.text.takeIf { it.isNotEmpty() }
            ?: DarkMonolithConfig.DEFAULT_TITLE
        TitleManager.sendTitle(titleText, duration = 3.seconds)
    }

    private fun drawDisplay(data: Data): List<Searchable> = buildList {
        addSearchString("§5§lDark Monolith Tracker")
        val profit = tracker.drawItems(data, { true }, this)
        add(
            StringRenderable(
                "§7Monoliths looted: §d${data.monolithsLooted}",
            ).toSearchable(),
        )
        add(tracker.addTotalProfit(profit, data.monolithsLooted, "loot"))
        tracker.addPriceFromButton(this)
    }

    @HandleEvent(onlyOnIsland = IslandType.DWARVEN_MINES)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.highlight.enabled) return
        val axis = renderBox ?: return
        with(WorldRenderUtils) {
            event.drawFilledBoundingBox(axis, config.highlight.color.toColor())
        }
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Dark Monolith")
        event.addIrrelevant {
            add("knownEggs: ${knownEggs.size}")
            add("knownEggs can be seen: ${knownEggs.count { it.canBeSeen() }}")
            add("foundEggVec: $foundEggVec")
            add("lastFoundEggVec: $lastFoundEggVec")
            add("renderBox: $renderBox")
        }
    }

    private fun anyEnabled() = config.tracker || config.highlight.enabled || config.title.enabled
}
