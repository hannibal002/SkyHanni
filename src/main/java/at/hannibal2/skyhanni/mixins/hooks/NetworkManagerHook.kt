package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.events.PacketEvent
import io.netty.channel.ChannelHandlerContext
import net.minecraft.network.Packet
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

fun onReceivePacket(context: ChannelHandlerContext, packet: Packet<*>, ci: CallbackInfo) {
    if (packet != null) {
        if (PacketEvent.ReceiveEvent(packet).postAndCatch()) ci.cancel()
    }
}