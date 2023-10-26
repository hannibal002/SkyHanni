package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ViewRecipeCommand {
    private val config get() = SkyHanniMod.feature.commands

    @SubscribeEvent
    fun onSendPacket(event: PacketEvent.SendEvent) {
        if (!config.viewRecipeLowerCase) return
        val packet = event.packet
        if (packet is C01PacketChatMessage) {
            val message = packet.message
            if (message == message.uppercase()) return
            if (message.startsWith("/viewrecipe ", ignoreCase = true)) {
                event.isCanceled = true
                LorenzUtils.sendMessageToServer(message.uppercase())
            }
        }
    }
}
