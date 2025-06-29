package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.misc.ParticleHider;
import net.minecraft.entity.mob.BlazeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlazeEntity.class)
public class MixinEntityBlaze {

    @Redirect(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addParticleClient(Lnet/minecraft/particle/ParticleEffect;DDDDDD)V"))
    private void onLivingUpdate(net.minecraft.world.World world, net.minecraft.particle.ParticleEffect particleType, double x, double y, double z, double xOffset, double yOffset, double zOffset) {
        if (!ParticleHider.shouldHideBlazeParticles()) {
            world.addParticleClient(particleType, x, y, z, xOffset, yOffset, zOffset);
        }
    }

}
