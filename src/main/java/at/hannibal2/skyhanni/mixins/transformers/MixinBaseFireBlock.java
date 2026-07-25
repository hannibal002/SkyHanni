package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.misc.ParticleHider;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BaseFireBlock.class)
public class MixinBaseFireBlock {

    @WrapOperation(
        method = "animateTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"
        )
    )
    private void onAddParticle(
        Level level,
        ParticleOptions particle,
        double x,
        double y,
        double z,
        double xd,
        double yd,
        double zd,
        Operation<Void> original
    ) {
        if (!ParticleHider.shouldHideFireParticles()) original.call(level, particle, x, y, z, xd, yd, zd);
    }
}
