package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.misc.ParticleHider;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Blaze.class)
public class MixinEntityBlaze {

    @Inject(
        method = "aiStep",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V",
        ),
        cancellable = true
    )
    private void onLivingUpdate(
        Level level,
        ParticleOptions particle,
        double x,
        double y,
        double z,
        double xd,
        double yd,
        double zd,
        CallbackInfo ci
    ) {
        if (ParticleHider.shouldHideBlazeParticles()) ci.cancel();
    }
}
