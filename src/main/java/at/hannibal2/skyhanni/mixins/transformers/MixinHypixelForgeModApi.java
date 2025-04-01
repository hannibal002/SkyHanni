package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.misc.CurrentPing;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(targets = "net.hypixel.modapi.forge.ForgeModAPI$HypixelPacketHandler")
public class MixinHypixelForgeModApi {

    @Inject(
        method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/Packet;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/network/PacketBuffer;retain()Lio/netty/buffer/ByteBuf;"),
        cancellable = true,
        locals = LocalCapture.CAPTURE_FAILSOFT,
        remap = false
    )
    private void fixOnChannelRead(ChannelHandlerContext ctx, Packet<?> msg, CallbackInfo ci, S3FPacketCustomPayload packet, String identifier, PacketBuffer buffer) {
        if (buffer.refCnt() <= 0) {
            CurrentPing.fixedModApiKickMessage();
            ci.cancel();
        }
    }
}
