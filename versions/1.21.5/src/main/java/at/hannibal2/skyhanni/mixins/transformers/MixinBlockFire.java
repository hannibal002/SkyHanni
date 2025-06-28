package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.block.AbstractFireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractFireBlock.class)
public class MixinBlockFire {

    @Redirect(method = "randomDisplayTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addParticleClient(Lnet/minecraft/particle/ParticleEffect;DDDDDD)V"))
    private void onRandomDisplayTick(net.minecraft.world.World world, net.minecraft.particle.ParticleEffect particleType, double x, double y, double z, double xOffset, double yOffset, double zOffset) {
        if (!at.hannibal2.skyhanni.SkyHanniMod.feature.misc.particleHiders.hideFireBlockParticles) {
            world.addParticleClient(particleType, x, y, z, xOffset, yOffset, zOffset);
        }
    }

}
