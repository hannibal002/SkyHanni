package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.garden.pests.PestFinderConfig.VisibilityType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.garden.pests.PestUpdateEvent
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.GardenPlotApi
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.isPestCountInaccurate
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.isPlayerInside
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.name
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.pests
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.renderPlot
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.sendTeleportTo
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.exactPlayerEyeLocation
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.StringRenderable
import net.minecraft.client.Minecraft
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object PestFinder {

    private val config get() = PestApi.config.pestFinder

    private var display = emptyList<Renderable>()

    @HandleEvent
    fun onPestUpdate(event: PestUpdateEvent) {
        update()
    }

    private fun update() {
        if (isEnabled()) {
            display = drawDisplay()
        }
    }

    private fun drawDisplay() = buildList {
        add(StringRenderable("§6Total pests: §e${PestApi.scoreboardPests}§6/§e8"))

        for (plot in PestApi.getInfestedPlots()) {
            val pests = plot.pests
            val plotName = plot.name
            val isInaccurate = plot.isPestCountInaccurate
            val pestsName = StringUtils.pluralize(pests, "pest")
            val name = "§e" + if (isInaccurate) "1+?" else {
                pests
            } + " §c$pestsName §7in §b$plotName"
            val renderable = Renderable.clickable(
                name,
                tips = listOf(
                    "§7Pests Found: §e" + if (isInaccurate) "Unknown" else pests,
                    "§7In plot §b$plotName",
                    "",
                    "§eClick here to warp!",
                ),
                onLeftClick = {
                    plot.sendTeleportTo()
                },
            )
            add(renderable)
        }

        if (PestApi.getInfestedPlots().isEmpty() && PestApi.scoreboardPests != 0) {
            remindInChat()
            add(StringRenderable("§e${PestApi.scoreboardPests} §6Bugged pests!"))
            add(
                Renderable.clickable(
                    "§cTry opening your plots menu",
                    tips = listOf(
                        "Runs /desk.",
                    ),
                    onLeftClick = {
                        HypixelCommands.gardenDesk()
                    },
                ),
            )
            add(
                Renderable.clickable(
                    "§cor enable Pests Widget in §e/widget.",
                    tips = listOf(
                        "Runs /widget.",
                    ),
                    onLeftClick = {
                        HypixelCommands.widget()
                    },
                ),
            )
        }
    }

    private fun remindInChat() {
        if (!TabWidget.PESTS.isActive) {
            ChatUtils.userError(
                "Pest detection requires the tab list widget to be enabled. Enable the 'Pests Widget' via /widget!",
                replaceSameMessage = true,
            )
        }
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        display = listOf()
        update()
    }

    init {
        RenderDisplayHelper(
            outsideInventory = true,
            inOwnInventory = true,
            condition = { shouldShowDisplay() },
            onRender = {
                config.position.renderRenderables(display, posLabel = "Pest Finder")
            },
        )
    }

    private fun shouldShowDisplay(): Boolean {
        if (!isEnabled()) return false
        if (!config.showDisplay) return false
        if (config.onlyWithVacuum && !PestApi.hasVacuumInHand()) return false

        return true
    }

    private fun heldItemDisabled() = config.onlyWithVacuum && !PestApi.hasVacuumInHand()
    private fun timePassedDisabled() = PestApi.lastTimeVacuumHold.passedSince() > config.showBorderForSeconds.seconds

    // priority to low so that this happens after other renderPlot calls.
    @HandleEvent(priority = HandleEvent.LOW)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return
        if (!config.showPlotInWorld) return
        if (heldItemDisabled() && timePassedDisabled()) return

        val playerLocation = event.exactPlayerEyeLocation()
        val visibility = config.visibilityType
        val showBorder = visibility == VisibilityType.BOTH || visibility == VisibilityType.BORDER
        val showName = visibility == VisibilityType.BOTH || visibility == VisibilityType.NAME
        for (plot in PestApi.getInfestedPlots()) {
            if (plot.isPlayerInside()) {
                if (showBorder) {
                    event.renderPlot(plot, LorenzColor.RED.toColor(), LorenzColor.DARK_RED.toColor())
                }
                continue
            }
            if (showBorder) {
                event.renderPlot(plot, LorenzColor.GOLD.toColor(), LorenzColor.RED.toColor())
            }
            if (showName) {
                drawName(plot, playerLocation, event)
            }
        }
    }

    private fun drawName(
        plot: GardenPlotApi.Plot,
        playerLocation: LorenzVec,
        event: SkyHanniRenderWorldEvent,
    ) {
        val pests = plot.pests
        val pestsName = StringUtils.pluralize(pests, "pest")
        val plotName = plot.name
        val middle = plot.middle
        val isInaccurate = plot.isPestCountInaccurate
        val location = playerLocation.copy(x = middle.x, z = middle.z)
        event.drawWaypointFilled(location, LorenzColor.RED.toColor())
        val number = if (isInaccurate) "?" else pests
        event.drawDynamicText(location, "§e$number §c$pestsName §7in §b$plotName", 1.5)
    }

    private var lastKeyPress = SimpleTimeMark.farPast()

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onChat(event: SkyHanniChatEvent) {
        if (!config.noPestTitle) return

        if (PestApi.noPestsChatPattern.matches(event.message)) TitleManager.sendTitle("§eNo pests!", duration = 2.seconds)
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onKeyPress(event: KeyPressEvent) {
        if (Minecraft.getMinecraft().currentScreen != null) return
        if (NeuItems.neuHasFocus()) return

        if (event.keyCode != config.teleportHotkey) return
        if (lastKeyPress.passedSince() < 2.seconds) return
        lastKeyPress = SimpleTimeMark.now()

        teleportNearestInfestedPlot()
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onPlaySound(event: PlaySoundEvent) {
        if (config.muteVacuum && event.soundName == "mob.wither.shoot") {
            event.cancel()
        }
    }

    private fun teleportNearestInfestedPlot() {
        // need to check again for the command
        if (!GardenApi.inGarden()) {
            ChatUtils.userError("This command only works while on the Garden!")
        }

        val plot = PestApi.getNearestInfestedPlot() ?: run {
            if (config.backToGarden) return HypixelCommands.warp("garden")

            ChatUtils.userError("No infested plots detected to warp to!")
            return
        }

        if (plot.isPlayerInside() && !config.alwaysTp) {
            ChatUtils.userError("You're already in an infested plot!")
            return
        }

        plot.sendTeleportTo()
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shtpinfested") {
            description = "Teleports you to the nearest infested plot"
            category = CommandCategory.USERS_ACTIVE
            simpleCallback { teleportNearestInfestedPlot() }
        }
    }

    fun isEnabled() = GardenApi.inGarden() && (config.showDisplay || config.showPlotInWorld)
}
