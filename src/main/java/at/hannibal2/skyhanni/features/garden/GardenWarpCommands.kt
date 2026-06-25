package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.events.minecraft.KeyDownEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft

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
            return
        }

        if (message == "/barn") {
            event.cancel()
            HypixelCommands.teleportToPlot("barn")
            return
        }

        tpPlotPattern.matchMatcher(message) {
            event.cancel()
            val plotName = group("plot")
            HypixelCommands.teleportToPlot(plotName)
            return
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onKeyDown(event: KeyDownEvent) {
        if (Minecraft.getInstance().screen != null) return

        when (event.keyCode) {
            config.homeHotkey -> HypixelCommands.warp("garden")
            config.sethomeHotkey -> HypixelCommands.setSpawn()
            config.barnHotkey -> HypixelCommands.teleportToPlot("barn")
            else -> return
        }
    }
}
