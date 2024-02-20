package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.events.LorenzKeyPressEvent
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.features.misc.LockMouseLook
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class GardenWarpCommands {

    private val config get() = GardenAPI.config.gardenCommands

    private val tpPlotPattern by RepoPattern.pattern(
        "garden.warpcommand.tpplot",
        "/tp (?<plot>.*)"
    )

    private var lastWarpTime = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onMessageSendToServer(event: MessageSendToServerEvent) {
        if (!config.warpCommands) return
        if (!GardenAPI.inGarden()) return

        val message = event.message.lowercase()

        if (message == "/home") {
            event.isCanceled = true
            ChatUtils.sendCommandToServer("warp garden")
            ChatUtils.chat("§aTeleported you to the spawn location!", prefix = false)
        }

        if (message == "/barn") {
            event.isCanceled = true
            ChatUtils.sendCommandToServer("tptoplot barn")
            LockMouseLook.autoDisable()
        }

        tpPlotPattern.matchMatcher(event.message) {
            event.isCanceled = true
            val plotName = group("plot")
            ChatUtils.sendCommandToServer("tptoplot $plotName")
            LockMouseLook.autoDisable()
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
        if (lastWarpTime.passedSince() < 2.seconds) return
        lastWarpTime = SimpleTimeMark.now()
        if (command == "tptoplot barn") {
            LockMouseLook.autoDisable()
        }
        ChatUtils.sendCommandToServer(command)
    }
}
