package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.entity.EntityEnterWorldEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import at.hannibal2.skyhanni.features.misc.ParticleHider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

@Mixin(ClientLevel.class)
public class MixinClientWorld {

    @Inject(method = "addEntity", at = @At("HEAD"))
    private void onAddEntity(Entity entity, CallbackInfo ci) {
        new EntityEnterWorldEvent(entity).post();
    }

     @Inject(method = "addDestroyBlockEffect", at = @At("HEAD"), cancellable = true)
     private void onAddBlockBreakParticles(BlockPos blockPos, BlockState blockState, CallbackInfo ci) {
         if (ParticleHider.shouldHideBlockParticles()) {
             ci.cancel();
         }
     }

     @Inject(method = "addBreakingBlockEffect", at = @At("HEAD"), cancellable = true)
     private void onAddBlockBreakingParticles(BlockPos blockPos, Direction direction, CallbackInfo ci) {
         if (ParticleHider.shouldHideBlockParticles()) {
             ci.cancel();
         }
     }

}
