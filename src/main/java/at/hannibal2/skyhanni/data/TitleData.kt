package at.hannibal2.skyhanni.data import at.hannibal2.skyhanni.utils.compat.formattedTextCompat

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.TitleReceivedEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.network.packet.s2c.play.TitleS2CPacket
//#if MC > 1.21
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket
//#endif

@SkyHanniModule
object TitleData {

    @HandleEvent
    fun onReceiveCurrentShield(event: PacketReceivedEvent) {
        val text = when (val packet = event.packet) {
            is TitleS2CPacket -> packet.text ?: return
            //#if MC > 1.21
            is SubtitleS2CPacket -> packet.text
            //#endif
            else -> return
        }

        val formattedText = text.formattedTextCompat()
        if (TitleReceivedEvent(formattedText).post()) {
            event.cancel()
        }
    }
}
