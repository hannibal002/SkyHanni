package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.misc.ParticleHider;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractHurtingProjectile.class)
public class MixinAbstractHurtingProjectile {

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "createParticleTrail", at = @At("HEAD"), cancellable = true)
    private void onCreateParticleTrail(CallbackInfo ci) {
        if ((Object) this instanceof Fireball && ParticleHider.shouldHideFireballParticles()) {
            ci.cancel();
        }
    }
}
