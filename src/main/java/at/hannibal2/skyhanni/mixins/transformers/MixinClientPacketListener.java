package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.ParticleChangeEvent;
import at.hannibal2.skyhanni.events.ReceiveParticleEvent;
import at.hannibal2.skyhanni.features.misc.CurrentPing;
import at.hannibal2.skyhanni.utils.LorenzVec;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/DebugScreenOverlay;showNetworkCharts()Z"))
    public boolean shouldShowPacketSizeAndPingCharts(boolean original) {
        if (!CurrentPing.INSTANCE.isEnabled()) return original;
        return true;
    }

    @Inject(
        method = "handleParticleEvent",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
            shift = At.Shift.AFTER
        ),
        cancellable = true
    )
    public void postParticleEvent(ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
        if (
            new ReceiveParticleEvent(
                packet.getParticle().getType(),
                new LorenzVec(packet.getX(), packet.getY(), packet.getZ()),
                packet.getCount(),
                packet.getMaxSpeed(),
                new LorenzVec(packet.getXDist(), packet.getYDist(), packet.getZDist()),
                packet.isOverrideLimiter(),
                null
            ).post()
        ) {
            ci.cancel();
        }
    }

    @ModifyArg(method = "handleParticleEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;addParticle(Lnet/minecraft/core/particles/ParticleOptions;ZZDDDDDD)V"))
    public ParticleOptions postParticleChangeEvent(ParticleOptions particleOptions, @Local(argsOnly = true) ClientboundLevelParticlesPacket packet) {
        ParticleChangeEvent particleChangeEvent = new ParticleChangeEvent(particleOptions, packet);
        particleChangeEvent.post();
        return particleChangeEvent.getParticleOptions();
    }

}
