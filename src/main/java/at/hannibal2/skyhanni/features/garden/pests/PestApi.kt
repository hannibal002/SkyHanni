package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.ItemInHandChangeEvent
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.garden.pests.PestKillEvent
import at.hannibal2.skyhanni.events.garden.pests.PestSpawnEvent
import at.hannibal2.skyhanni.events.garden.pests.PestUpdateEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.GardenPlotApi
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.isBarn
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.isPestCountInaccurate
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.locked
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.name
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.pests
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.uncleared
import at.hannibal2.skyhanni.features.garden.pests.PestProfitTracker.DUNG_ITEM
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStack
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.entity.decoration.ArmorStand
import org.lwjgl.glfw.GLFW
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object PestApi {

    val config get() = GardenApi.config.pests
    val storage get() = GardenApi.storage
    private val SPRAYONATOR_ITEM = "SPRAYONATOR".toInternalName()

    var scoreboardPests: Int
        get() = storage?.scoreboardPests ?: 0
        set(value) {
            storage?.scoreboardPests = value
        }

    private var lastPestKillTime = SimpleTimeMark.farPast()
    var lastPestSpawnTime = SimpleTimeMark.farPast()
    var lastTimeVacuumHeld = SimpleTimeMark.farPast()
    var lastTimeLassoHeld = SimpleTimeMark.farPast()

    fun hasVacuumInHand() = InventoryUtils.getItemInHand()?.getItemCategoryOrNull() == ItemCategory.VACUUM
    fun hasLassoInHand() = InventoryUtils.getItemInHand()?.getItemCategoryOrNull() == ItemCategory.LASSO
    fun hasSprayonatorInHand() = InventoryUtils.itemInHandId == SPRAYONATOR_ITEM

    fun SprayType.getPests() = PestType.filterableEntries.filter { it.spray == this }

    val patternGroup = RepoPattern.group("garden.pests-api")
    private val pestsInScoreboardPattern by patternGroup.pattern(
        "scoreboard.pests",
        " §7⏣ §[ac]The Garden §4§lൠ§7 x(?<pests>.*)",
    )

    /**
     * REGEX-TEST:  §7⏣ §aPlot §7- §b22a
     * REGEX-TEST:  §7⏣ §aThe Garden
     */
    private val noPestsInScoreboardPattern by patternGroup.pattern(
        "scoreboard.no-pests",
        " §7⏣ §a(?:The Garden|Plot §7- §b.+)$",
    )

    /**
     * REGEX-TEST:    §aPlot §7- §b4 §4§lൠ§7 x1
     */
    private val pestsInPlotScoreboardPattern by patternGroup.pattern(
        "scoreboard.plot.pests",
        "\\s*(?:§.)*Plot (?:§.)*- (?:§.)*(?<plot>.+) (?:§.)*ൠ(?:§.)* x(?<pests>\\d+)",
    )

    /**
     * REGEX-TEST:  §aPlot §7- §b3
     */
    private val noPestsInPlotScoreboardPattern by patternGroup.pattern(
        "scoreboard.plot.no-pests",
        "\\s*(?:§.)*Plot (?:§.)*- (?:§.)*(?<plot>.{1,3})$",
    )
    private val pestInventoryPattern by patternGroup.pattern(
        "inventory",
        "§4§lൠ §cThis plot has §6(?<amount>\\d) Pests?§c!",
    )

    /**
     * REGEX-TEST:  Plots: §r§b4§r§f, §r§b12§r§f, §r§b13§r§f, §r§b18§r§f, §r§b20
     */
    private val infestedPlotsTabListPattern by patternGroup.pattern(
        "tablist.infected-plots",
        "\\sPlots: (?<plots>.*)",
    )

    /**
     * REGEX-TEST: §eYou received §a7x Enchanted Potato §efor killing a §2Locust§e!
     * REGEX-TEST: §eYou received §a6x Enchanted Cocoa Beans §efor killing a §2Moth§e!
     * REGEX-TEST: §eYou received §a64x Enchanted Sugar §efor killing a §2Mosquito§e!
     */
    val pestDeathChatPattern by patternGroup.pattern(
        "chat.pest-death",
        "§eYou received §a(?<amount>[0-9]*)x (?<item>.*) §efor killing an? §2(?<pest>.*)§e!",
    )
    val noPestsChatPattern by patternGroup.pattern(
        "chat.no-pests",
        "§cThere are not any Pests on your Garden right now! Keep farming!",
    )

    /**
     * REGEX-TEST: §9§lPEST TRAP #3§r
     * REGEX-TEST: §5§lMOUSE TRAP #2§r
     * REGEX-TEST: §6§lVERMIN TRAP #2
     */
    private val pestTrapPattern by patternGroup.pattern(
        "entity.pest-trap",
        "(?:§.)+§l(?<type>PEST|MOUSE|VERMIN) TRAP(?: #(?<number>\\d+))?(?:§.)*",
    )

    /**
     * REGEX-TEST: Stereo Harmony
     */
    private val stereoInventoryPattern by patternGroup.pattern(
        "stereo.inventory",
        "Stereo Harmony"
    )
    val stereoInventory = InventoryDetector { name -> stereoInventoryPattern.matches(name) }

    /**
     * REGEX-TEST: §7Now Playing: §aWings of Harmony §8(Moth)
     * REGEX-TEST: §7Now Playing: §a§cNone
     */
    val stereoPlayingPattern by patternGroup.pattern(
        "stereo.playing",
        "§7Now Playing: (?:§.)*(?<vinyl>[^§]+).*"
    )

    /**
     * REGEX-TEST: PLAYING
     */
    val stereoPlayingItemPattern by patternGroup.pattern(
        "stereo.playing.item",
        "PLAYING",
    )

    private var gardenJoinTime = SimpleTimeMark.farPast()
    private var firstScoreboardCheck = false

    private fun fixPests(loop: Int = 2) {
        DelayedRun.runDelayed(2.seconds) {
            val accurateAmount = getPlotsWithAccuratePests().sumOf { it.pests }
            val inaccurate = getPlotsWithInaccuratePests()
            val inaccurateAmount = inaccurate.size
            when {
                // if we can assume all inaccurate plots have 1 pest each
                scoreboardPests == accurateAmount + inaccurateAmount -> {
                    for (plot in inaccurate) {
                        plot.pests = 1
                        plot.isPestCountInaccurate = false
                    }
                }
                // if we can assume all the inaccurate pests are in the only inaccurate plot
                inaccurateAmount == 1 -> {
                    val plot = inaccurate.first()
                    plot.pests = scoreboardPests - accurateAmount
                    plot.isPestCountInaccurate = false
                }
                // when logic fails and we reach impossible pest counts
                accurateAmount + inaccurateAmount > scoreboardPests -> {
                    getInfestedPlots().forEach {
                        it.pests = 0
                        it.isPestCountInaccurate = true
                    }
                    if (loop > 0) fixPests(loop - 1)
                    else sendPestError()
                }
            }
        }
    }

    private fun updatePests() {
        if (!firstScoreboardCheck) return
        fixPests()
        PestUpdateEvent.post()
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onPestSpawn(event: PestSpawnEvent) {
        val plotNames = event.plotNames
        for (plotName in plotNames) {
            val plot = GardenPlotApi.getPlotByName(plotName)
            if (plot == null) {
                ChatUtils.userError("Open Plot Management Menu to load plot names and pest locations!")
                return
            }
            plot.isPestCountInaccurate = event.amountPests?.let {
                plot.pests += it
                false
            } ?: true
        }
        event.amountPests?.let { scoreboardPests += it }
        updatePests()
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (event.inventoryName != "Configure Plots") return

        for (plot in GardenPlotApi.plots) {
            if (plot.isBarn() || plot.locked || plot.uncleared) continue
            plot.pests = 0
            plot.isPestCountInaccurate = false
            val item = event.inventoryItems[plot.inventorySlot] ?: continue
            pestInventoryPattern.firstMatcher(item.getLore()) {
                plot.pests = group("amount").toInt()
            }
        }
        updatePests()
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onWidgetUpdate(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.PESTS)) return

        infestedPlotsTabListPattern.firstMatcher(event.widget.lines) {
            val tabListPlots = group("plots").removeColor().split(", ").map { it.toInt() }.toSet()
            val apiPlots = getInfestedPlots().map { it.id }.toSet()

            if (tabListPlots == apiPlots) return

            for (plot in GardenPlotApi.plots) {
                if (plot.id in tabListPlots) {
                    if (!plot.isPestCountInaccurate && plot.pests == 0) {
                        plot.isPestCountInaccurate = true
                    }
                } else {
                    plot.pests = 0
                    plot.isPestCountInaccurate = false
                }
            }
            updatePests()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onScoreboardChange(event: ScoreboardUpdateEvent) {
        if (!firstScoreboardCheck) return
        checkScoreboardLines(event.added)
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onChat(event: SkyHanniChatEvent) {
        pestDeathChatPattern.matchMatcher(event.message) {
            val pest = PestType.getByNameOrNull(group("pest")) ?: return
            val item = NeuInternalName.fromItemNameOrNull(group("item")) ?: return

            // Field Mice drop 6 separate items, but we only want to count the kill once
            if (pest == PestType.FIELD_MOUSE && item != DUNG_ITEM) return
            lastPestKillTime = SimpleTimeMark.now()
            removeNearestPest()
            PestKillEvent.post()
        }
        if (noPestsChatPattern.matches(event.message)) {
            resetAllPests()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onTick() {
        if (!firstScoreboardCheck && gardenJoinTime.passedSince() > 5.seconds) {
            checkScoreboardLines(ScoreboardData.sidebarLinesFormatted)
            firstScoreboardCheck = true
            updatePests()
        }
    }

    @HandleEvent
    fun onWorldChange() {
        lastPestKillTime = SimpleTimeMark.farPast()
        lastTimeVacuumHeld = SimpleTimeMark.farPast()
        lastTimeLassoHeld = SimpleTimeMark.farPast()
        gardenJoinTime = SimpleTimeMark.now()
        firstScoreboardCheck = false
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onItemInHandChange(event: ItemInHandChangeEvent) {
        if (event.oldItem.getItemStack().getItemCategoryOrNull() == ItemCategory.VACUUM) {
            lastTimeVacuumHeld = SimpleTimeMark.now()
        }
        if (event.oldItem.getItemStack().getItemCategoryOrNull() == ItemCategory.LASSO) {
            lastTimeLassoHeld = SimpleTimeMark.now()
        }
    }

    private fun getPlotsWithAccuratePests() = GardenPlotApi.plots.filter { it.pests > 0 && !it.isPestCountInaccurate }

    private fun getPlotsWithInaccuratePests() = GardenPlotApi.plots.filter { it.isPestCountInaccurate }

    fun getInfestedPlots() = GardenPlotApi.plots.filter { it.pests > 0 || it.isPestCountInaccurate }

    fun getPlotsWithoutPests() = GardenPlotApi.plots.filter { it.pests == 0 || !it.isPestCountInaccurate }

    fun getNearestInfestedPlot() = getInfestedPlots().minByOrNull { it.middle.distanceSqToPlayer() }

    fun isNearPestTrap() = EntityUtils.getEntitiesNextToPlayer<ArmorStand>(10.0).any {
        pestTrapPattern.matches(it.displayName.formattedTextCompat())
    }

    private fun removePests(removedPests: Int) {
        if (removedPests < 1) return
        repeat(removedPests) {
            removeNearestPest()
        }
    }

    private fun removeNearestPest() {
        val plot = getNearestInfestedPlot() ?: return updatePests()
        if (!plot.isPestCountInaccurate) plot.pests--
        scoreboardPests--
        updatePests()
    }

    private fun resetAllPests() {
        scoreboardPests = 0
        GardenPlotApi.plots.forEach {
            it.pests = 0
            it.isPestCountInaccurate = false
        }
        updatePests()
    }

    private fun sendPestError() {
        ErrorManager.logErrorStateWithData(
            "Error getting pest count",
            "Impossible pest count",
            "scoreboardPests" to scoreboardPests,
            "plots" to getInfestedPlots().map { "id: ${it.id} pests: ${it.pests} isInaccurate: ${it.isPestCountInaccurate}" },
            noStackTrace = true,
            betaOnly = true,
        )
    }

    private fun checkScoreboardLines(list: List<String>) {
        for (line in list) {
            // gets if there are no pests remaining in the garden
            noPestsInScoreboardPattern.matchMatcher(line) {
                if (scoreboardPests != 0 || getInfestedPlots().isNotEmpty()) {
                    resetAllPests()
                }
                return
            }

            // gets the total amount of pests in the garden
            pestsInScoreboardPattern.matchMatcher(line) {
                val newPests = group("pests").formatInt()
                if (newPests != scoreboardPests) {
                    scoreboardPests = newPests
                    updatePests()
                }
            }

            // gets the amount of pests in the current plot
            pestsInPlotScoreboardPattern.matchMatcher(line) {
                val plotName = group("plot")
                val pestsInPlot = group("pests").toInt()
                val plot = GardenPlotApi.getPlotByName(plotName) ?: return
                if (pestsInPlot != plot.pests || plot.isPestCountInaccurate) {
                    plot.pests = pestsInPlot
                    plot.isPestCountInaccurate = false
                    updatePests()
                }
            }

            // gets if there are no pests remaining in the current plot
            noPestsInPlotScoreboardPattern.matchMatcher(line) {
                val plotName = group("plot")
                val plot = GardenPlotApi.getPlotByName(plotName) ?: return
                if (plot.pests != 0 || plot.isPestCountInaccurate) {
                    plot.pests = 0
                    plot.isPestCountInaccurate = false
                    updatePests()
                }
            }
        }
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Garden Pests")

        if (!GardenApi.inGarden()) {
            event.addIrrelevant("not in garden")
            return
        }
        val disabled = with(config.pestFinder) {
            !showDisplay && !showPlotInWorld && teleportHotkey == GLFW.GLFW_KEY_UNKNOWN
        }
        if (disabled) {
            event.addIrrelevant("disabled in config")
            return
        }

        event.addIrrelevant {
            add("scoreboardPests is $scoreboardPests")
            add("")
            getInfestedPlots().forEach {
                add("id: ${it.id}")
                add(" name: ${it.name}")
                add(" isPestCountInaccurate: ${it.isPestCountInaccurate}")
                add(" pests: ${it.pests}")
                add(" ")
            }
        }
    }
}
