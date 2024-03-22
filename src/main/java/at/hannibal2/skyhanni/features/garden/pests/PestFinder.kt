package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.ItemInHandChangeEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzKeyPressEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.ScoreboardChangeEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.events.garden.pests.PestSpawnEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.getPlotByName
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.isBarn
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.isPestCountInaccurate
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.isPlayerInside
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.name
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.pests
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.renderPlot
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.sendTeleportTo
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.RenderUtils.exactPlayerEyeLocation
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class PestFinder {

    private val config get() = PestAPI.config.pestFinder

    private val patternGroup = RepoPattern.group("garden.pests.finder")
    private val pestsInScoreboardPattern by patternGroup.pattern(
        "scoreboard",
        " §7⏣ §[ac]The Garden §4§lൠ§7 x(?<pests>.*)"
    )
    /**
     * REGEX-TEST:  §7⏣ §aPlot §7- §b22a
     * REGEX-TEST:  §7⏣ §aThe Garden
     */
    private val noPestsInScoreboardPattern by patternGroup.pattern(
        "scoreboardnopests",
        " §7⏣ §a(?:The Garden|Plot §7- §b.+)$"
    )
    /**
     * REGEX-TEST:    §aPlot §7- §b4 §4§lൠ§7 x1
     */
    private val pestsInPlotScoreboardPattern by patternGroup.pattern(
        "scoreboard.plot.haspests",
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
     * REGEX-TEST:  Infested Plots: §r§b4§r§f, §r§b12§r§f, §r§b13§r§f, §r§b18§r§f, §r§b20
     */
    private val infectedPlotsTablistPattern by patternGroup.pattern(
        "tablist.infectedplots",
        "\\sInfested Plots: (?<plots>.*)"
    )


    private var display = emptyList<Renderable>()
    private var lastTimeVacuumHold = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onPestSpawn(event: PestSpawnEvent) {
        if (!isEnabled()) return
        PestSpawnTimer.lastSpawnTime = SimpleTimeMark.now()
        val plotNames = event.plotNames
        for (plotName in plotNames) {
            val plot = getPlotByName(plotName)
            if (plot == null) {
                ChatUtils.userError("Open Plot Management Menu to load plot names and pest locations!")
                return
            }
            if (event.unkownAmount) {
                plot.isPestCountInaccurate = true
            } else {
                plot.pests += event.amountPests
                plot.isPestCountInaccurate = false
            }
        }
        update()
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!GardenAPI.inGarden()) return
        if (event.inventoryName != "Configure Plots") return

        for (plot in GardenPlotAPI.plots) {
            if (!plot.isBarn()) {
                plot.pests = 0
                plot.isPestCountInaccurate = false
                val item = event.inventoryItems[plot.inventorySlot] ?: continue
                for (line in item.getLore()) {
                    pestInventoryPattern.matchMatcher(line) {
                        plot.pests = group("amount").formatNumber().toInt()
                    }
                }
            }
        }
        fixPests()
        update()
    }

    private fun update() {
        if (isEnabled()) {
            display = drawDisplay()
        }
    }

    private fun fixPests() {
        val accurateAmount = getPlotsWithAccuratePests().sumOf { it.pests }
        val inaccurateAmount = getPlotsWithInaccuratePests().size
        if (PestAPI.scoreboardPests == accurateAmount + inaccurateAmount) { // if we can assume all inaccurate plots have 1 pest each
            for (plot in getPlotsWithInaccuratePests()) {
                plot.pests = 1
                plot.isPestCountInaccurate = false
            }
        }
        if (inaccurateAmount == 1) { // if we can assume all the inaccurate pests are in the only inaccurate plot
            val plot = getPlotsWithInaccuratePests()[0]
            plot.pests = PestAPI.scoreboardPests - accurateAmount
            plot.isPestCountInaccurate = false
        }
        update()
    }

    private fun drawDisplay() = buildList {
        add(Renderable.string("§6Total pests in garden: §e${PestAPI.scoreboardPests}§6/§e8"))

        for (plot in getPlotsWithAllPests()) {
            val pests = plot.pests
            val plotName = plot.name
            val isInaccurate = plot.isPestCountInaccurate
            val pestsName = StringUtils.pluralize(pests, "pest")
            val renderable = Renderable.clickAndHover(
                "§e" + if (isInaccurate) "1+?" else {pests} + " §c$pestsName §7in §b$plotName",
                listOf(
                    "§7Pests Found: §e" + if (isInaccurate) "Unknown" else pests,
                    "§7In plot §b$plotName",
                    "",
                    "§eClick here to warp!"
                ),
                onClick = {
                    plot.sendTeleportTo()
                }
            )
            add(renderable)
        }

        if (getPlotsWithAllPests().isEmpty() && PestAPI.scoreboardPests != 0) {
            add(Renderable.string("§e${PestAPI.scoreboardPests} §6Bugged pests!"))
            add(Renderable.clickAndHover(
                "§cTry opening your plots menu.",
                listOf(
                    "Runs /desk."
                ),
                onClick = {
                    ChatUtils.sendCommandToServer("desk")
                }
            ))
        }
    }

    @SubscribeEvent
    fun onIslandChange(event: IslandChangeEvent) {
        update()
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
        if (event.message == "§cThere are not any Pests on your Garden right now! Keep farming!") {
            GardenPlotAPI.plots.forEach {
                it.pests = 0
                it.isPestCountInaccurate = false
            }
            update()
        }
    }

    @SubscribeEvent
    fun onScoreboardChange(event: ScoreboardChangeEvent) {
        if (!GardenAPI.inGarden()) return

        for (line in event.newList) {
            pestsInScoreboardPattern.matchMatcher(line) {
                val newPests = group("pests").formatNumber().toInt()
                if (newPests != PestAPI.scoreboardPests) {
                    removePests(PestAPI.scoreboardPests - newPests)
                    PestAPI.scoreboardPests = newPests
                    fixPests()
                    update()
                }
            }
            noPestsInScoreboardPattern.matchMatcher(line) {
                if (PestAPI.scoreboardPests != 0) {
                    PestAPI.scoreboardPests = 0
                    GardenPlotAPI.plots.forEach {
                        it.pests = 0
                        it.isPestCountInaccurate = false
                    }
                    update()
                }
            }

            pestsInPlotScoreboardPattern.matchMatcher(line) {
                val plotName = group("plot")
                val pestsInPlot = group("pests").toInt()
                val plot = getPlotByName(plotName)
                if (pestsInPlot != plot?.pests || plot.isPestCountInaccurate) {
                    plot?.pests = pestsInPlot
                    plot?.isPestCountInaccurate = false
                    fixPests()
                    update()
                }
            }
            noPestsInPlotScoreboardPattern.matchMatcher(line) {
                val plotName = group("plot")
                val plot = getPlotByName(plotName)
                if (plot?.pests != 0 || plot.isPestCountInaccurate) {
                    getPlotByName(plotName)?.pests = 0
                    getPlotByName(plotName)?.isPestCountInaccurate = false
                    fixPests()
                    update()
                }
            }
        }
        //resetAllPests(newPests)
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
                fixPests()
                update()
            }
        }
    }

    private fun removePests(removedPests: Int) {
        if (removedPests < 1) return
        repeat(removedPests) {
            removeNearestPest()
        }
    }

    private fun getNearestInfestedPlot() = getPlotsWithAllPests().minByOrNull { it.middle.distanceSqToPlayer() }

    private fun removeNearestPest() {
        val plot = getNearestInfestedPlot() ?: run {
            ChatUtils.error("Can not remove nearest pest: No infested plots detected.")
            return
        }
        if (!plot.isPestCountInaccurate) plot.pests--
        update()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return
        if (!config.showDisplay) return
        if (config.onlyWithVacuum && !PestAPI.hasVacuumInHand()) return

        if (GardenAPI.inGarden() && config.showDisplay) {
            config.position.renderRenderables(display, posLabel = "Pest Finder")
        }
    }

    private fun getPlotsWithAccuratePests() = GardenPlotAPI.plots.filter { it.pests > 0 && !it.isPestCountInaccurate }

    private fun getPlotsWithInaccuratePests() = GardenPlotAPI.plots.filter { it.pests == 0 && it.isPestCountInaccurate }

    private fun getPlotsWithAllPests() = GardenPlotAPI.plots.filter { it.pests > 0 || it.isPestCountInaccurate }

    private fun getPlotsWithoutPests() = GardenPlotAPI.plots.filter { it.pests == 0 || !it.isPestCountInaccurate }

    // priority to low so that this happens after other renderPlot calls.
    @SubscribeEvent(priority = EventPriority.LOW)
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        if (!config.showPlotInWorld) return
        if (config.onlyWithVacuum && !PestAPI.hasVacuumInHand() && (lastTimeVacuumHold.passedSince() > config.showBorderForSeconds.seconds)) return

        val playerLocation = event.exactPlayerEyeLocation()
        for (plot in getPlotsWithAllPests()) {
            if (plot.isPlayerInside()) {
                event.renderPlot(plot, LorenzColor.RED.toColor(), LorenzColor.DARK_RED.toColor())
                continue
            }
            event.renderPlot(plot, LorenzColor.GOLD.toColor(), LorenzColor.RED.toColor())

            val pests = plot.pests
            val pestsName = StringUtils.pluralize(pests, "pest")
            val plotName = plot.name
            val middle = plot.middle
            val isInaccurate = plot.isPestCountInaccurate
            val location = playerLocation.copy(x = middle.x, z = middle.z)
            event.drawWaypointFilled(location, LorenzColor.RED.toColor())
            event.drawDynamicText(location, "§f" + if (isInaccurate) "?" else {pests} + " §c$pestsName §7in §b$plotName", 1.5)
        }
    }

    private var lastKeyPress = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onKeyClick(event: LorenzKeyPressEvent) {
        if (!GardenAPI.inGarden()) return
        if (Minecraft.getMinecraft().currentScreen != null) return
        if (NEUItems.neuHasFocus()) return

        if (event.keyCode != config.teleportHotkey) return
        if (lastKeyPress.passedSince() < 2.seconds) return
        lastKeyPress = SimpleTimeMark.now()

        val plot = getNearestInfestedPlot() ?: run {
            ChatUtils.userError("No infested plots detected to warp to!")
            return
        }

        if (plot.isPlayerInside()) {
            ChatUtils.userError("You're already in an infested plot!")
            return
        }

        plot.sendTeleportTo()
    }

    @SubscribeEvent
    fun onItemInHandChange(event: ItemInHandChangeEvent) {
        if (!isEnabled()) return
        if (!config.showPlotInWorld) return
        if (event.oldItem !in PestAPI.vacuumVariants) return
        lastTimeVacuumHold = SimpleTimeMark.now()
    }

    fun isEnabled() = GardenAPI.inGarden() && (config.showDisplay || config.showPlotInWorld)
}
