package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SendCoordinatedCommand {

    @SubscribeEvent
    fun onSendPacket(event: PacketEvent.SendEvent) {
        val packet = event.packet
        if (packet is C01PacketChatMessage) {
            val message = packet.message.lowercase()
            if (message == "/sendcoords") {
                event.isCanceled = true
                LorenzUtils.sendMessageToServer(getCoordinates())
            } else if (message.startsWith("/sendcoords ")) {
                event.isCanceled = true
                val description = message.split(" ").drop(1).joinToString(" ")
                LorenzUtils.sendMessageToServer("${getCoordinates()} $description")
            }
        }
    }

    private fun getCoordinates(): String {
        val location = LocationUtils.playerLocation()
        val x = location.x.toInt()
        val y = location.y.toInt()
        val z = location.z.toInt()
        return "x: $x, y: $y, z: $z"
    }


}