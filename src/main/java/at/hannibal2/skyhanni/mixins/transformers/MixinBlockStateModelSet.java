package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.BlockStateModelSetHookKt;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

//? if >= 26.1 {
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
//?} else {
/*import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockStateModel;
*///?}

//~ if < 26.1 'BlockStateModelSet' -> 'BlockRenderDispatcher'
@Mixin(BlockStateModelSet.class)
public class MixinBlockStateModelSet {

    //~ if < 26.1 'get' -> 'getBlockModel'
    @ModifyReturnValue(method = "get", at = @At("RETURN"))
    public BlockStateModel getModel(BlockStateModel original, BlockState state) {
        BlockStateModel newModel = BlockStateModelSetHookKt.getModelOverride(
            //~ if < 26.1 'BlockStateModelSet' -> 'BlockRenderDispatcher'
            (BlockStateModelSet) (Object) this, state
        );
        return newModel != null ? newModel : original;
    }
}
