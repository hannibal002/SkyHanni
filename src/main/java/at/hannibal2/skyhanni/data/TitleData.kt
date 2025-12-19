package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.TitleReceivedEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket

@SkyHanniModule
object TitleData {

    @HandleEvent
    fun onReceiveCurrentShield(event: PacketReceivedEvent) {
        val text = when (val packet = event.packet) {
            is ClientboundSetTitleTextPacket -> packet.text ?: return
            is ClientboundSetSubtitleTextPacket -> packet.text
            else -> return
        }

        val formattedText = text.formattedTextCompat()
        if (TitleReceivedEvent(formattedText).post()) {
            event.cancel()
        }
    }
}
