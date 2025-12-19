package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.misc.ParticleHider;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

@Mixin(ParticleEngine.class)
public class MixinEffectRenderer {

    @Inject(method = "destroy", at = @At("HEAD"), cancellable = true)
    private void onAddBlockBreakParticles(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (ParticleHider.shouldHideBlockParticles()) {
            ci.cancel();
        }
    }

    @Inject(method = "crack", at = @At("HEAD"), cancellable = true)
    private void onAddBlockBreakingParticles(BlockPos pos, Direction side, CallbackInfo ci) {
        if (ParticleHider.shouldHideBlockParticles()) {
            ci.cancel();
        }
    }
}
