package at.hannibal2.skyhanni.mixins.transformers

import at.hannibal2.skyhanni.events.PacketEvent
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import net.minecraft.network.NetworkManager
import net.minecraft.network.Packet
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(value = [NetworkManager::class], priority = 1001)
abstract class MixinNetworkManager : SimpleChannelInboundHandler<Packet<*>?>() {
    @Inject(method = ["channelRead0"], at = [At("HEAD")], cancellable = true)
    private fun onReceivePacket(context: ChannelHandlerContext, packet: Packet<*>, ci: CallbackInfo) {
        if (PacketEvent.ReceiveEvent(packet).postAndCatch()) ci.cancel()
    }
}