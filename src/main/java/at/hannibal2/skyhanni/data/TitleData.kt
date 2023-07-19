package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.events.TitleReceivedEvent
import net.minecraft.network.play.server.S45PacketTitle
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class TitleData {

    @SubscribeEvent
    fun onReceiveCurrentShield(event: PacketEvent.ReceiveEvent) {
        val packet = event.packet

        if (packet !is S45PacketTitle) return
        val message = packet.message ?: return
        val formattedText = message.formattedText
        if (TitleReceivedEvent(formattedText).postAndCatch()) {
            event.isCanceled = true
        }
    }
}