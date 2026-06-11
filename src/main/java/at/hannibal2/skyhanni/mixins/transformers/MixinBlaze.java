package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.misc.ParticleHider;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Blaze.class)
public class MixinBlaze {

    @Inject(
        method = "aiStep",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"
        ),
        cancellable = true)
    private void onAddParticle(CallbackInfo ci) {
        if (ParticleHider.shouldHideBlazeParticles()) ci.cancel();
    }
}
