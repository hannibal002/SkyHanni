package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.config.features.misc.BlockBreakParticleConfig;
import at.hannibal2.skyhanni.data.IslandType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import at.hannibal2.skyhanni.SkyHanniMod;

//#if MC > 1.21
//$$import net.minecraft.client.particle.ParticleManager;
//$$import net.minecraft.block.BlockState;
//$$import net.minecraft.util.math.BlockPos;
//$$import net.minecraft.util.math.Direction;
//#else
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
//#endif

//#if MC > 1.21
//$$ @Mixin(ParticleManager.class)
//#else
@Mixin(EffectRenderer.class)
//#endif
public class MixinEffectRenderer {

    //#if MC > 1.21
    //$$@Inject(
    //$$    method = "addBlockBreakParticles",
    //$$    at = @At("HEAD"),
    //$$    cancellable = true
    //$$)
    //$$private void onAddBlockBreakParticles(BlockPos pos, BlockState state, CallbackInfo ci) {
    //$$    BlockBreakParticleConfig config = SkyHanniMod.feature.misc.particleHiders.getBlockBreakParticleConfig();
    //$$
    //$$    if (config.hideBlockBreakParticles) {
    //$$        if (config.hideBlockBreakParticlesGarden) {
    //$$            if (IslandType.GARDEN.isCurrent()) {
    //$$                ci.cancel();
    //$$            }
    //$$        } else {
    //$$            ci.cancel();
    //$$        }
    //$$    }
    //$$}
    //#else
    @Inject(method = "addBlockDestroyEffects", at = @At("HEAD"), cancellable = true)
    private void onAddBlockDestroyEffects(BlockPos pos, IBlockState state, CallbackInfo ci) {
        BlockBreakParticleConfig config = SkyHanniMod.feature.misc.particleHiders.getBlockBreakParticleConfig();

        if (config.hideBlockBreakParticles){
            if (config.hideBlockBreakParticlesGarden){
                if (IslandType.GARDEN.isCurrent()){
                    ci.cancel();
                }
            } else {
                ci.cancel();
            }
        }

    }
    //#endif

    //#if MC > 1.21
    //$$@Inject(
    //$$    method = "addBlockBreakingParticles",
    //$$    at = @At("HEAD"),
    //$$    cancellable = true
    //$$)
    //$$private void onAddBlockBreakingParticles(BlockPos pos, Direction side, CallbackInfo ci) {
    //$$    BlockBreakParticleConfig config = SkyHanniMod.feature.misc.particleHiders.getBlockBreakParticleConfig();
    //$$
    //$$    if (config.hideBlockBreakParticles) {
    //$$        if (config.hideBlockBreakParticlesGarden) {
    //$$            if (IslandType.GARDEN.isCurrent()) {
    //$$                ci.cancel();
    //$$            }
    //$$        } else {
    //$$            ci.cancel();
    //$$        }
    //$$    }
    //$$}
    //#else
    @Inject(method = "addBlockHitEffects*", at = @At("HEAD"), cancellable = true)
    private void onAddBlockHitEffects(BlockPos pos, EnumFacing side, CallbackInfo ci) {
        BlockBreakParticleConfig config = SkyHanniMod.feature.misc.particleHiders.getBlockBreakParticleConfig();

        if (config.hideBlockBreakParticles){
            if (config.hideBlockBreakParticlesGarden){
                if (IslandType.GARDEN.isCurrent()){
                    ci.cancel();
                }
            } else {
                ci.cancel();
            }
        }
    }
    //#endif
}
