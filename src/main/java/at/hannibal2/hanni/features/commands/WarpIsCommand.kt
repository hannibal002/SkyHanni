package at.hannibal2.hanni.features.commands

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.MessageSendToServerEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.HypixelCommands

@HanniModule
object WarpIsCommand {

    @HandleEvent(onlyOnSkyblock = true)
    fun onMessageSendToServer(event: MessageSendToServerEvent) {
        if (!HanniMod.feature.misc.commands.replaceWarpIs) return

        if (event.message.lowercase() == "/warp is") {
            event.cancel()
            HypixelCommands.island()
        }
    }
}
