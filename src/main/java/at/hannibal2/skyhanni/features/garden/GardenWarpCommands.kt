package at.hannibal2.hanni.features.garden

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.MessageSendToServerEvent
import at.hannibal2.hanni.events.minecraft.KeyDownEvent
import at.hannibal2.hanni.features.garden.sensitivity.LockMouseLook
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.HypixelCommands
import at.hannibal2.hanni.utils.NeuItems
import at.hannibal2.hanni.utils.RegexUtils.matchMatcher
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import kotlin.time.Duration.Companion.seconds

@HanniModule
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

    private var lastWarpTime = SimpleTimeMark.farPast()

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

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onKeyDown(event: KeyDownEvent) {
        if (Minecraft.getMinecraft().currentScreen != null) return
        if (NeuItems.neuHasFocus()) return

        when (event.keyCode) {
            config.homeHotkey -> {
                if (lastWarpTime.passedSince() < 2.seconds) return
                lastWarpTime = SimpleTimeMark.now()

                HypixelCommands.warp("garden")
            }

            config.sethomeHotkey -> {
                HypixelCommands.setHome()
            }

            config.barnHotkey -> {
                if (lastWarpTime.passedSince() < 2.seconds) return
                lastWarpTime = SimpleTimeMark.now()

                LockMouseLook.unlockMouse()
                HypixelCommands.teleportToPlot("barn")
            }

            else -> return
        }
    }
}
