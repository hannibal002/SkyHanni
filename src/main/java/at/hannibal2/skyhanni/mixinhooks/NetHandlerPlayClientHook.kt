package at.hannibal2.skyhanni.mixinhooks

import at.hannibal2.skyhanni.events.PacketEvent
import net.minecraft.network.Packet
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

fun onSendPacket(packet: Packet<*>, ci: CallbackInfo) {
    if (PacketEvent.SendEvent(packet).postAndCatch()) ci.cancel()
}