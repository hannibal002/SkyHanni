package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.ScoreboardChangeEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.events.garden.pests.PestSpawnEvent
import at.hannibal2.skyhanni.events.garden.pests.PestUpdateEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.isBarn
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.isPestCountInaccurate
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.locked
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.pests
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.uncleared
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object PestAPI {

    val config get() = GardenAPI.config.pests

    var scoreboardPests = 0

    val vacuumVariants = listOf(
        "SKYMART_VACUUM".asInternalName(),
        "SKYMART_TURBO_VACUUM".asInternalName(),
        "SKYMART_HYPER_VACUUM".asInternalName(),
        "INFINI_VACUUM".asInternalName(),
        "INFINI_VACUUM_HOOVERIUS".asInternalName(),
    )

    fun hasVacuumInHand() = InventoryUtils.itemInHandId in vacuumVariants

    fun SprayType.getPests() = PestType.entries.filter { it.spray == this }

    private val patternGroup = RepoPattern.group("garden.pestsapi")
    private val pestsInScoreboardPattern by patternGroup.pattern(
        "scoreboard.pests",
        " §7⏣ §[ac]The Garden §4§lൠ§7 x(?<pests>.*)"
    )
    /**
     * REGEX-TEST:  §7⏣ §aPlot §7- §b22a
     * REGEX-TEST:  §7⏣ §aThe Garden
     */
    private val noPestsInScoreboardPattern by patternGroup.pattern(
        "scoreboard.nopests",
        " §7⏣ §a(?:The Garden|Plot §7- §b.+)$"
    )
    /**
     * REGEX-TEST:    §aPlot §7- §b4 §4§lൠ§7 x1
     */
    private val pestsInPlotScoreboardPattern by patternGroup.pattern(
        "scoreboard.plot.pests",
        "\\s*(?:§.)*Plot (?:§.)*- (?:§.)*(?<plot>.+) (?:§.)*ൠ(?:§.)* x(?<pests>\\d+)"
    )
    /**
     * REGEX-TEST:  §aPlot §7- §b3
     */
    private val noPestsInPlotScoreboardPattern by patternGroup.pattern(
        "scoreboard.plot.nopests",
        "\\s*(?:§.)*Plot (?:§.)*- (?:§.)*(?<plot>.{1,3})$"
    )
    private val pestInventoryPattern by patternGroup.pattern(
        "inventory",
        "§4§lൠ §cThis plot has §6(?<amount>\\d) Pests?§c!"
    )
    /**
     * REGEX-TEST:  Plots: §r§b4§r§f, §r§b12§r§f, §r§b13§r§f, §r§b18§r§f, §r§b20
     */
    private val infectedPlotsTablistPattern by patternGroup.pattern(
        "tablist.infectedplots",
        "\\sPlots: (?<plots>.*)"
    )

    private fun fixPests() {
        val accurateAmount = getPlotsWithAccuratePests().sumOf { it.pests }
        val inaccurateAmount = getPlotsWithInaccuratePests().size
        if (scoreboardPests == accurateAmount + inaccurateAmount) { // if we can assume all inaccurate plots have 1 pest each
            for (plot in getPlotsWithInaccuratePests()) {
                plot.pests = 1
                plot.isPestCountInaccurate = false
            }
        } else if (inaccurateAmount == 1) { // if we can assume all the inaccurate pests are in the only inaccurate plot
            val plot = getPlotsWithInaccuratePests().firstOrNull()
            plot?.pests = scoreboardPests - accurateAmount
            plot?.isPestCountInaccurate = false
        }
    }

    private fun updatePests() {
        fixPests()
        PestUpdateEvent().postAndCatch()
    }

    @SubscribeEvent
    fun onPestSpawn(event: PestSpawnEvent) {
        if (!GardenAPI.inGarden()) return
        PestSpawnTimer.lastSpawnTime = SimpleTimeMark.now()
        val plotNames = event.plotNames
        for (plotName in plotNames) {
            val plot = GardenPlotAPI.getPlotByName(plotName)
            if (plot == null) {
                ChatUtils.userError("Open Plot Management Menu to load plot names and pest locations!")
                return
            }
            if (event.unknownAmount) {
                plot.isPestCountInaccurate = true
            } else {
                plot.pests += event.amountPests
                plot.isPestCountInaccurate = false
            }
        }
        updatePests()
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!GardenAPI.inGarden()) return
        if (event.inventoryName != "Configure Plots") return

        for (plot in GardenPlotAPI.plots) {
            if (plot.isBarn() || plot.locked || plot.uncleared) continue
            plot.pests = 0
            plot.isPestCountInaccurate = false
            val item = event.inventoryItems[plot.inventorySlot] ?: continue
            for (line in item.getLore()) {
                pestInventoryPattern.matchMatcher(line) {
                    plot.pests = group("amount").toInt()
                }
            }
        }
        updatePests()
    }

    @SubscribeEvent
    fun onTablistUpdate(event: TabListUpdateEvent) {
        if (!GardenAPI.inGarden()) return
        var previousLine = ""
        for (line in event.tabList) {
            infectedPlotsTablistPattern.matchMatcher(line) {
                if (previousLine == line) return
                val plotList = mutableListOf<Int>()
                group("plots").removeColor().split(", ").toMutableList().forEach {
                    plotList.add(it.toInt())
                }

                GardenPlotAPI.plots.forEach {
                    if (plotList.contains(it.id)) {
                        if (!it.isPestCountInaccurate && it.pests == 0) {
                            it.isPestCountInaccurate = true
                        }
                    } else {
                        it.pests = 0
                        it.isPestCountInaccurate = false
                    }
                }
                previousLine = line
                updatePests()
            }
        }
    }

    @SubscribeEvent
    fun onScoreboardChange(event: ScoreboardChangeEvent) {
        if (!GardenAPI.inGarden()) return

        for (line in event.newList) {
            // gets the total amount of pests in the garden
            pestsInScoreboardPattern.matchMatcher(line) {
                val newPests = group("pests").formatNumber().toInt()
                if (newPests != scoreboardPests) {
                    removePests(scoreboardPests - newPests)
                    scoreboardPests = newPests
                    updatePests()
                }
            }

            // gets if there are no pests remaining in the garden
            noPestsInScoreboardPattern.matchMatcher(line) {
                if (scoreboardPests != 0) {
                    resetAllPests()
                }
            }

            // gets the amount of pests in the current plot
            pestsInPlotScoreboardPattern.matchMatcher(line) {
                val plotName = group("plot")
                val pestsInPlot = group("pests").toInt()
                val plot = GardenPlotAPI.getPlotByName(plotName)
                if (pestsInPlot != plot?.pests || plot.isPestCountInaccurate) {
                    plot?.pests = pestsInPlot
                    plot?.isPestCountInaccurate = false
                    updatePests()
                }
            }

            // gets if there are no pests remaining in the current plot
            noPestsInPlotScoreboardPattern.matchMatcher(line) {
                val plotName = group("plot")
                val plot = GardenPlotAPI.getPlotByName(plotName)
                if (plot?.pests != 0 || plot.isPestCountInaccurate) {
                    GardenPlotAPI.getPlotByName(plotName)?.pests = 0
                    GardenPlotAPI.getPlotByName(plotName)?.isPestCountInaccurate = false
                    updatePests()
                }
            }
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!GardenAPI.inGarden()) return
        if (event.message == "§cThere are not any Pests on your Garden right now! Keep farming!") {
            resetAllPests()
        }
    }

    private fun getPlotsWithAccuratePests() = GardenPlotAPI.plots.filter { it.pests > 0 && !it.isPestCountInaccurate }

    private fun getPlotsWithInaccuratePests() = GardenPlotAPI.plots.filter { it.pests == 0 && it.isPestCountInaccurate }

    fun getInfestedPlots() = GardenPlotAPI.plots.filter { it.pests > 0 || it.isPestCountInaccurate }

    fun getPlotsWithoutPests() = GardenPlotAPI.plots.filter { it.pests == 0 || !it.isPestCountInaccurate }

    fun getNearestInfestedPlot() = getInfestedPlots().minByOrNull { it.middle.distanceSqToPlayer() }

    private fun removePests(removedPests: Int) {
        if (removedPests < 1) return
        repeat(removedPests) {
            removeNearestPest()
        }
    }

    private fun removeNearestPest() {
        val plot = getNearestInfestedPlot() ?: run {
            ChatUtils.error("Can not remove nearest pest: No infested plots detected.")
            return
        }
        if (!plot.isPestCountInaccurate) plot.pests--
        updatePests()
    }

    private fun resetAllPests() {
        scoreboardPests = 0
        GardenPlotAPI.plots.forEach {
            it.pests = 0
            it.isPestCountInaccurate = false
        }
        updatePests()
    }
}
