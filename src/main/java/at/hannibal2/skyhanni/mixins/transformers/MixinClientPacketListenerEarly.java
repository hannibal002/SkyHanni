package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.utils.ParticleUtils;
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
        ParticleUtils.postParticleEvent(packet);
    }
}
