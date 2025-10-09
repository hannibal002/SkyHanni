package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.misc.ParticleHider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

@Mixin(EffectRenderer.class)
public class MixinEffectRenderer {

    @Inject(method = "addBlockDestroyEffects", at = @At("HEAD"), cancellable = true)
    private void onAddBlockDestroyEffects(BlockPos pos, IBlockState state, CallbackInfo ci) {
        if (ParticleHider.shouldHideBlockParticles()) {
            ci.cancel();
        }
    }

    @Inject(method = "addBlockHitEffects*", at = @At("HEAD"), cancellable = true)
    private void onAddBlockHitEffects(BlockPos pos, EnumFacing side, CallbackInfo ci) {
        if (ParticleHider.shouldHideBlockParticles()) {
            ci.cancel();
        }
    }

}
