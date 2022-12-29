package at.hannibal2.skyhanni.mixins.transformers.renderer;

import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockRendererDispatcher.class)
public abstract class MixinBlockRendererDispatcher implements IResourceManagerReloadListener {
//    @Inject(method = "getModelFromBlockState", at = @At("RETURN"), cancellable = true)
//    private void modifyGetModelFromBlockState(IBlockState state, IBlockAccess worldIn, BlockPos pos, CallbackInfoReturnable<IBakedModel> cir) {
////        BlockRendererDispatcherHookKt.modifyGetModelFromBlockState(this, state, worldIn, pos, cir);
//    }
}
