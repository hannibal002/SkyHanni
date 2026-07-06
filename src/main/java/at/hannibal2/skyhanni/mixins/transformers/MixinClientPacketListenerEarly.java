package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.ParticleDetectedEvent;
import at.hannibal2.skyhanni.utils.LorenzVec;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientPacketListener.class, priority = 0)
public class MixinClientPacketListenerEarly {
    @Inject(at = @At("HEAD"), method = "handleParticleEvent")
    public void handleParticleEventEarly(ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
        new ParticleDetectedEvent(
            packet.getParticle().getType(),
            new LorenzVec(packet.getX(), packet.getY(), packet.getZ()),
            packet.getCount(),
            packet.getMaxSpeed(),
            new LorenzVec(packet.getXDist(), packet.getYDist(), packet.getZDist()),
            packet.isOverrideLimiter(),
            null
        ).post();
    }
}
