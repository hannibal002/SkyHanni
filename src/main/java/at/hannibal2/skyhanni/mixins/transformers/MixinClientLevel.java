package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.entity.EntityEnterWorldEvent;
import at.hannibal2.skyhanni.features.misc.ParticleHider;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class MixinClientLevel {
    @Inject(method = "addEntity", at = @At("HEAD"))
    private void onAddEntity(Entity entity, CallbackInfo ci) {
        new EntityEnterWorldEvent<>(entity).post();
    }

    @Inject(method = "addDestroyBlockEffect", at = @At("HEAD"), cancellable = true)
    private void onAddBlockBreakParticles(CallbackInfo ci) {
        if (ParticleHider.shouldHideBlockParticles()) {
            ci.cancel();
        }
    }

    //~ if < 26.3 'addBreakingParticles' -> 'addBreakingBlockEffect'
    @Inject(method = "addBreakingParticles", at = @At("HEAD"), cancellable = true)
    private void onAddBlockBreakingParticles(CallbackInfo ci) {
        if (ParticleHider.shouldHideBlockParticles()) {
            ci.cancel();
        }
    }
}
