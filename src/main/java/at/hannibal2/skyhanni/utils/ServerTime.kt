package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.network.protocol.game.ClientboundSetTimePacket

@SkyHanniModule
object ServerTime {
    /**
     * The current in-game day time reported by the server,
     * unaffected by time-changing mods with invasive mixins.
     */
    @JvmStatic
    var dayTime: Long = 0L
        private set

    @HandleEvent
    internal fun onPacketReceived(event: PacketReceivedEvent) {
        val packet = event.packet as? ClientboundSetTimePacket ?: return
        dayTime = packet.dayTime
    }
}
