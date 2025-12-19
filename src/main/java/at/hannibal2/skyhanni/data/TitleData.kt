package at.hannibal2.skyhanni.data import at.hannibal2.skyhanni.utils.compat.formattedTextCompat

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.TitleReceivedEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
//#if MC > 1.21
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
//#endif

@SkyHanniModule
object TitleData {

    @HandleEvent
    fun onReceiveCurrentShield(event: PacketReceivedEvent) {
        val packet = event.packet

        val text = when (packet) {
            is ClientboundSetTitleTextPacket -> packet.text ?: return
            //#if MC > 1.21
            is ClientboundSetSubtitleTextPacket -> packet.text
            //#endif
            else -> return
        }

        val formattedText = text.formattedTextCompat()
        if (TitleReceivedEvent(formattedText).post()) {
            event.cancel()
        }
    }
}
