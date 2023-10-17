package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ViewRecipeCommand {

    @SubscribeEvent
    fun onSendPacket(event: PacketEvent.SendEvent) {
        val packet = event.packet
        if (packet is C01PacketChatMessage) {
            val message = packet.message.lowercase()
            if (message.startsWith("/viewrecipe ")) {
                event.isCanceled = true
                LorenzUtils.sendMessageToServer(message.uppercase())
            }
        }
    }

}