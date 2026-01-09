package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.SkyHanniMod;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Fireball.class)
public class MixinEntityFireball {

    @Redirect(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;spawnParticle(Lnet/minecraft/core/particles/ParticleTypes;DDDDDD[I)V"))
    private void onLivingUpdate(Level world, ParticleTypes particleType, double x, double y, double z, double xOffset, double yOffset, double zOffset, int[] parameters) {
        if (!SkyHanniMod.feature.misc.particleHiders.hideFireballParticles) {
            world.spawnParticle(particleType, x, y, z, xOffset, yOffset, zOffset, parameters);
        }
    }
}
