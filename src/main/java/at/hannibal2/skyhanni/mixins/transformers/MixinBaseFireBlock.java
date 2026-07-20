package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.misc.ParticleHider;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BaseFireBlock.class)
public abstract class MixinBaseFireBlock {

    @Redirect(method = "animateTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void onRandomDisplayTick(Level world, ParticleOptions particleType, double x, double y, double z, double xOffset, double yOffset, double zOffset) {
        if (!ParticleHider.shouldHideFireParticles()) {
            world.addParticle(particleType, x, y, z, xOffset, yOffset, zOffset);
        }
    }
}
