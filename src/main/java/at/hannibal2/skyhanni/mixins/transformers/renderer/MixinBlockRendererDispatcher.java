package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.mixins.hooks.BlockRendererDispatcherHookKt;
import net.minecraft.world.level.block.state.BlockState;
//? if < 26.1 {
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
//? } else
// import net.minecraft.client.renderer.block.BlockStateModelSet;
//~ if > 1.21.11 'model.BlockStateModel' -> 'dispatch.BlockStateModel'
import net.minecraft.client.renderer.block.model.BlockStateModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// TODO 26.1 might be best to split this? idk if we can do full separate files per version with stone cutter
//? if < 26.1 {
@Mixin(BlockRenderDispatcher.class)
public class MixinBlockRendererDispatcher {
    @Inject(method = "getBlockModel", at = @At("HEAD"), cancellable = true)
    public void getModel(BlockState state, CallbackInfoReturnable<BlockStateModel> cir) {
        BlockRendererDispatcherHookKt.modifyGetModelFromBlockState((BlockRenderDispatcher) (Object) this, state, cir);
    }
}
//? } else {
/*@Mixin(BlockStateModelSet.class)
public class MixinBlockRendererDispatcher {
    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    public void getModel(BlockState state, CallbackInfoReturnable<BlockStateModel> cir) {
        BlockRendererDispatcherHookKt.modifyGetModelFromBlockState((BlockStateModelSet) (Object) this, state, cir);
    }
}*/
//?}
