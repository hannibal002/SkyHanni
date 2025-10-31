package at.hannibal2.hanni.data

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.TitleReceivedEvent
import at.hannibal2.hanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import net.minecraft.network.play.server.S45PacketTitle
//#if MC > 1.21
//$$ import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket
//#endif

@HanniModule
object TitleData {

    @HandleEvent
    fun onReceiveCurrentShield(event: PacketReceivedEvent) {
        val packet = event.packet

        val text = when (packet) {
            is S45PacketTitle -> packet.message ?: return
            //#if MC > 1.21
            //$$ is SubtitleS2CPacket -> packet.text
            //#endif
            else -> return
        }

        val formattedText = text.formattedText
        if (TitleReceivedEvent(formattedText).post()) {
            event.cancel()
        }
    }
}
