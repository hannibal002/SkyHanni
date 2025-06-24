package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent;
import at.hannibal2.skyhanni.events.minecraft.packet.PacketSentEvent;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class MixinClientConnection {

    @Inject(method = "handlePacket", at = @At(value = "HEAD"))
    private static void handlePacket$Inject$HEAD(Packet<?> packet, PacketListener listener, CallbackInfo ci) {
        new PacketReceivedEvent(packet).post();
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;Z)V", at = @At(value = "HEAD"), cancellable = true)
    private void sendPacketNew(Packet<?> packet, PacketCallbacks callbacks, boolean flush, CallbackInfo ci) {
        if (new PacketSentEvent(packet).post()) {
            ci.cancel();
        }
    }
}
