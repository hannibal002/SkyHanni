package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.mixins.hooks.BlockRendererDispatcherHookKt;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockRenderDispatcher.class)
public class MixinBlockRendererDispatcher {

    @Inject(method = "getBlockModel", at = @At("HEAD"), cancellable = true)
    public void getModel(BlockState state, CallbackInfoReturnable<BlockStateModel> cir) {
        BlockRendererDispatcherHookKt.modifyGetModelFromBlockState((BlockRenderDispatcher) (Object) this, state, cir);
    }
}
