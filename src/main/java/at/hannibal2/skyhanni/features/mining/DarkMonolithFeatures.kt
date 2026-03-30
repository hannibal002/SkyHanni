package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.mining.dwarves.monolith.DarkMonolithConfig
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.config.storage.Resettable
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.maxBox
import at.hannibal2.skyhanni.utils.LocationUtils.minBox
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderDisplayConfig
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawFilledBoundingBox
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.renderBeaconBeam
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.data.ItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyHanniItemTracker
import com.google.gson.annotations.Expose
import net.minecraft.core.Direction
import net.minecraft.world.level.block.Blocks.DRAGON_EGG
import net.minecraft.world.phys.AABB
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object DarkMonolithFeatures : SkyHanniItemTracker<DarkMonolithFeatures.Data>("Dark Monolith Tracker") {
    private val darkMonolithConfig get() = SkyHanniMod.feature.mining.darkMonolith
    override val config get() = darkMonolithConfig.tracker
    override val storageAccessor: (ProfileSpecificStorage) -> Data = { it.mining.darkMonolithTracker }
    override val renderConfig = RenderDisplayConfig(
        outsideInventory = true,
        inOwnInventory = true,
        condition = { config.enabled },
        onlyOnIsland = IslandType.DWARVEN_MINES,
    )

    class Data(
        @Expose var monolithsLooted: Long = 0,
    ) : ItemTrackerData<SessionUptime.Normal>() {
        override fun getDescription(timesGained: Long) = emptyList<String>()
        override fun getCoinName(item: TrackedItem) = "§6Monolith Coins"
        override fun getCoinDescription(item: TrackedItem) = emptyList<String>()
    }

    private val mithrilPowderItem = "SKYBLOCK_POWDER_MITHRIL".toInternalName()
    private val rockTheFishItem = "ROCK_THE_FISH".toInternalName()
    private val patternGroup = RepoPattern.group("mining.dwarves.darkmonolith")

    // Todo: need chat pattern for rock the fish drop
    /**
     * REGEX-TEST: MONOLITH! You found a mysterious Dark Monolith and were rewarded 50,000 Coins!
     * REGEX-TEST: MONOLITH! You found a mysterious Dark Monolith and were rewarded 2,500 Coins and 1,000 ᠅ Mithril Powder!
     * REGEX-TEST: MONOLITH! You found a mysterious Dark Monolith and were rewarded 100 ᠅ Mithril Powder!
     * REGEX-TEST: MONOLITH! You found a mysterious Dark Monolith and were rewarded 3,000 ᠅ Mithril Powder!
     */
    private val dropPattern by patternGroup.pattern(
        "drop.chat.colorless",
        "MONOLITH! You.*and were rewarded ?(?:(?<coins>[\\d,]+) Coins ?)?(?:!|and )?(?:(?<powder>[\\d,]+) ᠅ Mithril Powder!)?",
    )

    internal data class DarkMonolithStateData(
        var knownEggs: Set<LorenzVec> = setOf(),
        var foundEggVec: LorenzVec? = null,
        var lastFoundEggVec: LorenzVec? = null,
        var renderBox: AABB? = null,
        var nextBlockCheck: SimpleTimeMark = SimpleTimeMark.farPast(),
    ) : Resettable

    internal val data = DarkMonolithStateData()

    @HandleEvent(onlyOnIsland = IslandType.DWARVEN_MINES)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        dropPattern.matchMatcher(event.chatComponent) {
            data.reset()
            groupOrNull("coins")?.let {
                addCoins(it.formatInt(), false)
            }
            groupOrNull("powder")?.let {
                addItem(mithrilPowderItem, it.formatInt(), false)
            }
            groupOrNull("fish")?.let {
                addItem(rockTheFishItem, 1, false)
            }
            modify {
                it.monolithsLooted++
            }
        }
    }

    @HandleEvent
    fun onWorldChange(event: IslandChangeEvent) {
        data.reset()
        if (event.newIsland == IslandType.DWARVEN_MINES) {
            firstUpdate()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.DWARVEN_MINES)
    fun onSecondPassed() {
        if (!anyEnabled()) return
        data.knownEggs = BlockUtils.nearbyBlocks(
            LocationUtils.playerLocation(),
            distance = 40,
            filter = DRAGON_EGG,
        ).keys
    }

    @HandleEvent(onlyOnIsland = IslandType.DWARVEN_MINES)
    fun onTick() {
        if (!anyEnabled()) return
        data.updateKnownEggs()
        val knownEggVec = data.foundEggVec ?: return
        with(WorldRenderUtils) {
            data.renderBox = knownEggVec.boundingToOffset(1.0, 1.0, 1.0).expandBlock()
        }
    }

    private fun canSeeFaces(vec: LorenzVec): Boolean {
        val aabb = vec.floor().boundingToOffset(1.0, 1.0, 1.0)
        return LocationUtils.canSeeAnyFace(
            min = aabb.minBox(),
            max = aabb.maxBox(),
            stepCount = 4,
            ignoreFaces = listOf(Direction.DOWN).toTypedArray(),
        )
    }

    private fun DarkMonolithStateData.updateKnownEggs() {
        if (nextBlockCheck.isInFuture()) return
        foundEggVec = knownEggs.firstOrNull(::canSeeFaces)
        checkTitle()
        nextBlockCheck = SimpleTimeMark.now().plus(500.milliseconds)
    }

    private fun DarkMonolithStateData.checkTitle() {
        if (foundEggVec == null || foundEggVec == lastFoundEggVec) return
        lastFoundEggVec = foundEggVec
        val titleConfig = darkMonolithConfig.title
        if (!titleConfig.enabled) return
        val titleText = titleConfig.text.takeIf { it.isNotEmpty() }
            ?: DarkMonolithConfig.DEFAULT_TITLE
        TitleManager.sendTitle(titleText, duration = 3.seconds)
        ChatUtils.notifyOrDisable(titleText, titleConfig::enabled)
    }

    override fun drawDisplayF(data: Data): List<Searchable> = buildList {
        addSearchString("Dark Monolith Tracker")
        val profit = drawItems(data, { true }, this)
        add(Renderable.text("§7Monoliths looted: §d${data.monolithsLooted}").toSearchable())
        addAll(
            addTotalProfit(profit, data.monolithsLooted, "loot", data.getTotalUptime()),
        )
        addPriceFromButton(this)
    }

    @HandleEvent(onlyOnIsland = IslandType.DWARVEN_MINES)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        event.tryHighlight()
        event.tryBeacon()
    }

    private fun SkyHanniRenderWorldEvent.tryBeacon() = with(darkMonolithConfig.beacon) {
        if (!enabled) return
        val foundVec = data.foundEggVec ?: return
        renderBeaconBeam(foundVec, color.toColor())
    }

    private fun SkyHanniRenderWorldEvent.tryHighlight() = with(darkMonolithConfig.highlight) {
        if (!enabled) return
        val axis = data.renderBox ?: return
        drawFilledBoundingBox(axis, color.toColor())
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        val base = "mining.darkMonolith"
        event.move(130, "$base.tracker", "$base.tracker.enabled")
        event.move(130, "$base.trackerPosition", "$base.tracker.position")
        event.move(130, "$base.perTrackerConfig", "$base.perTrackerConfig.perTrackerConfig")
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Dark Monolith")
        event.addIrrelevant {
            add("knownEggs: ${data.knownEggs.size}")
            add("knownEggs can be seen: ${data.knownEggs.count(::canSeeFaces)}")
            add("foundEggVec: ${data.foundEggVec}")
            add("lastFoundEggVec: ${data.lastFoundEggVec}")
            add("renderBox: ${data.renderBox}")
        }
    }

    private fun anyEnabled() = with(darkMonolithConfig) {
        beacon.enabled || tracker.enabled || highlight.enabled || title.enabled
    }
}
