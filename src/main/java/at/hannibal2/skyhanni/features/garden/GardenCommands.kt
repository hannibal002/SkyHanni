package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.events.LorenzKeyPressEvent
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GardenCommands {
    private val config get() = GardenAPI.config.gardenCommands

    // TODO repo
    private val tpPlotPattern = "/tp (?<plot>.*)".toPattern()

    @SubscribeEvent
    fun onMessageSendToServer(event: MessageSendToServerEvent) {
        if (!config.warpCommands) return
        if (!GardenAPI.inGarden()) return

        val message = event.message.lowercase()

        if (message == "/home") {
            event.isCanceled = true
            LorenzUtils.sendCommandToServer("warp garden")
            LorenzUtils.chat("§aTeleported you to the spawn location!", prefix = false)
        }

        if (message == "/barn") {
            event.isCanceled = true
            LorenzUtils.sendCommandToServer("tptoplot barn")
        }

        tpPlotPattern.matchMatcher(message) {
            event.isCanceled = true
            val plotName = group("plot")
            LorenzUtils.sendCommandToServer("tptoplot $plotName")
        }
    }

    @SubscribeEvent
    fun onKeyClick(event: LorenzKeyPressEvent) {
        if (!GardenAPI.inGarden()) return
        if (Minecraft.getMinecraft().currentScreen != null) return
        if (NEUItems.neuHasFocus()) return

        val command = when (event.keyCode) {
            config.homeHotkey -> "warp garden"
            config.sethomeHotkey -> "sethome"
            config.barnHotkey -> "tptoplot barn"

            else -> return
        }
        LorenzUtils.sendCommandToServer(command)
    }
}
