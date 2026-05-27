package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.network.protocol.game.ClientboundSetTimePacket

@SkyHanniModule
object ServerTime {
    var dayTime: Long = 0L
        private set

    @HandleEvent
    fun onPacketReceived(event: PacketReceivedEvent) {
        val packet = event.packet as? ClientboundSetTimePacket ?: return
        dayTime = packet.dayTime
    }
}
