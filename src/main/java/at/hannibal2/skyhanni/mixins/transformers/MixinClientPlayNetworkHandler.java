package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.ParticleChangeEvent;
import at.hannibal2.skyhanni.features.misc.CurrentPing;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = ClientPacketListener.class)
public class MixinClientPlayNetworkHandler {

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/DebugScreenOverlay;showNetworkCharts()Z"))
    public boolean shouldShowPacketSizeAndPingCharts(boolean original) {
        if (!CurrentPing.INSTANCE.isEnabled()) return original;
        return true;
    }

    @ModifyArg(method = "handleParticleEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;addParticle(Lnet/minecraft/core/particles/ParticleOptions;ZZDDDDDD)V"))
    public ParticleOptions postParticleChangeEvent(ParticleOptions particleOptions, @Local(argsOnly = true) ClientboundLevelParticlesPacket packet) {
        ParticleChangeEvent particleChangeEvent = new ParticleChangeEvent(particleOptions, packet);
        particleChangeEvent.post();
        return particleChangeEvent.getParticleOptions();
    }
}
