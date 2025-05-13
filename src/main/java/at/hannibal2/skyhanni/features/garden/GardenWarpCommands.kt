package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.features.garden.sensitivity.LockMouseLook
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.Keybinding
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GardenWarpCommands {

    private val config get() = GardenApi.config.gardenCommands

    /**
     * REGEX-TEST: /tp 3
     * REGEX-TEST: /tp barn
     */
    private val tpPlotPattern by RepoPattern.pattern(
        "garden.warpcommand.tpplot",
        "/tp (?<plot>.*)",
    )

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onMessageSendToServer(event: MessageSendToServerEvent) {
        if (!config.warpCommands) return

        val message = event.message.lowercase()

        if (message == "/home") {
            event.cancel()
            HypixelCommands.warp("garden")
            ChatUtils.chat("§aTeleported you to the spawn location!", prefix = false)
        }

        if (message == "/barn") {
            event.cancel()
            HypixelCommands.teleportToPlot("barn")
            LockMouseLook.unlockMouse()
        }

        tpPlotPattern.matchMatcher(event.message) {
            event.cancel()
            val plotName = group("plot")
            HypixelCommands.teleportToPlot(plotName)
            LockMouseLook.unlockMouse()
        }
    }

    init {
        Keybinding(
            keyCodeProvider = { config.homeHotkey },
            functionToExecute = { HypixelCommands.warp("garden") },
            cooldown = 2.seconds,
            onlyOnIsland = IslandType.GARDEN,
            name = "Garden Home",
        )
        Keybinding(
            keyCodeProvider = { config.barnHotkey },
            functionToExecute = {
                LockMouseLook.unlockMouse()
                HypixelCommands.teleportToPlot("barn")
            },
            cooldown = 2.seconds,
            onlyOnIsland = IslandType.GARDEN,
            name = "Garden Barn",
        )
        Keybinding(
            keyCodeProvider = { config.sethomeHotkey },
            functionToExecute = { HypixelCommands.setHome() },
            onlyOnIsland = IslandType.GARDEN,
            name = "Garden Set Home",
        )
    }
}
