package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.mixins.hooks.BlockRendererDispatcherHookKt;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockRenderDispatcher.class)
public class MixinBlockRendererDispatcher {

    @ModifyReturnValue(method = "getBlockModel", at = @At("RETURN"))
    public BlockStateModel getModel(BlockStateModel original, BlockState state) {
        var newModel = BlockRendererDispatcherHookKt.modifyGetModelFromBlockState((BlockRenderDispatcher) (Object) this, state);
        return newModel != null ? newModel : original;
    }
}
