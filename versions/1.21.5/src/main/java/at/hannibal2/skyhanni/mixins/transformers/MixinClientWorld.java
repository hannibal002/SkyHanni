package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.entity.EntityEnterWorldEvent;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#if MC > 1.21.8
//$$ import at.hannibal2.skyhanni.features.misc.ParticleHider;
//$$ import net.minecraft.block.BlockState;
//$$ import net.minecraft.util.math.BlockPos;
//$$ import net.minecraft.util.math.Direction;
//#endif

@Mixin(ClientWorld.class)
public class MixinClientWorld {

    @Inject(method = "addEntity", at = @At("HEAD"))
    private void onAddEntity(Entity entity, CallbackInfo ci) {
        new EntityEnterWorldEvent(entity).post();
    }

    //#if MC > 1.21.8
    //$$ @Inject(method = "addBlockBreakParticles", at = @At("HEAD"), cancellable = true)
    //$$ private void onAddBlockBreakParticles(BlockPos pos, BlockState state, CallbackInfo ci) {
    //$$     if (ParticleHider.shouldHideBlockParticles()) {
    //$$         ci.cancel();
    //$$     }
    //$$ }
    //$$
    //$$ @Inject(method = "spawnBlockBreakingParticle", at = @At("HEAD"), cancellable = true)
    //$$ private void onAddBlockBreakingParticles(BlockPos pos, Direction side, CallbackInfo ci) {
    //$$     if (ParticleHider.shouldHideBlockParticles()) {
    //$$         ci.cancel();
    //$$     }
    //$$ }
    //#endif

}
