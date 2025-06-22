package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.BlockRenderManagerHookKt;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BlockStateModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockRenderManager.class)
public class MixinBlockRenderManager {

    @Inject(method = "getModel", at = @At("HEAD"), cancellable = true)
    public void getModel(BlockState state, CallbackInfoReturnable<BlockStateModel> cir) {
        BlockRenderManagerHookKt.modifyGetModelFromBlockState((BlockRenderManager) (Object) this, state, cir);
    }
}
