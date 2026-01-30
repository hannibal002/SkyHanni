package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent;
import at.hannibal2.skyhanni.events.minecraft.packet.PacketSentEvent;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.netty.channel.ChannelFutureListener;

@Mixin(Connection.class)
public class MixinClientConnection {

    @Inject(method = "genericsFtw", at = @At(value = "HEAD"), cancellable = true)
    private static void handlePacket$Inject$HEAD(Packet<?> packet, PacketListener listener, CallbackInfo ci) {
        if (new PacketReceivedEvent(packet).post()) {
            ci.cancel();
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At(value = "HEAD"), cancellable = true)
    private void sendPacketNew(Packet<?> packet, ChannelFutureListener channelFutureListener, boolean flush, CallbackInfo ci) {
        if (new PacketSentEvent(packet).post()) {
            ci.cancel();
        }
    }
}
