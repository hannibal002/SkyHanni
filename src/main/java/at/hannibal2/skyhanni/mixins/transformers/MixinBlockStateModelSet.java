//? if >= 26.1 {
package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.BlockRendererDispatcherHookKt;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockStateModelSet.class)
public class MixinBlockStateModelSet {
    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    public void getModel(BlockState state, CallbackInfoReturnable<BlockStateModel> cir) {
        BlockRendererDispatcherHookKt.modifyGetModelFromBlockState((BlockStateModelSet) (Object) this, state, cir);
    }
}
//?}
