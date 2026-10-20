package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.misc.CurrentPing;
import at.hannibal2.skyhanni.utils.ParticleUtils;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class MixinClientPacketListener {
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
        )
    )
    public void postParticleEvent(ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
        ParticleUtils.postParticleEvent(packet);
    }

    @WrapOperation(
        method = "handleParticleEvent",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel;addParticle(Lnet/minecraft/core/particles/ParticleOptions;ZZDDDDDD)V"
        )
    )
    public void wrapAddParticle(
        ClientLevel level,
        ParticleOptions particleOptions,
        boolean overrideLimiter,
        boolean alwaysShow,
        double x,
        double y,
        double z,
        double xd,
        double yd,
        double zd,
        Operation<Void> original,
        @Local(argsOnly = true) ClientboundLevelParticlesPacket packet
    ) {
        ParticleUtils.wrapAddParticle(level, particleOptions, overrideLimiter, alwaysShow, x, y, z, xd, yd, zd, original, packet);
    }
}
