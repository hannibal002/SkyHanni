package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.OSUtils
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SendCoordsCommand {

    @SubscribeEvent
    fun onSendPacket(event: PacketEvent.SendEvent) {
        val packet = event.packet
        if (packet is C01PacketChatMessage) {
            val message = packet.message.lowercase()
            if (message == "/sendcoords") {
                event.isCanceled = true
                LorenzUtils.sendMessageToServer("x: "+ LocationUtils.playerLocation().x.toInt() + ", y: " + LocationUtils.playerLocation().y.toInt() + ", z: " + LocationUtils.playerLocation().z.toInt())
            }
        }
    }


}