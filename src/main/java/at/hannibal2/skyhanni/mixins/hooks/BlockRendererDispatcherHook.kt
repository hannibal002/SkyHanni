package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.features.mining.MiningCommissionsBlocksColor
import at.hannibal2.skyhanni.utils.SkyBlockUtils
//~ if > 1.21.11 'BlockRenderDispatcher' -> 'BlockStateModelSet'
import net.minecraft.client.renderer.block.BlockStateModelSet
//~ if > 1.21.11 'model.BlockStateModel' -> 'dispatch.BlockStateModel'
import net.minecraft.client.renderer.block.dispatch.BlockStateModel
import net.minecraft.world.level.block.state.BlockState
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

fun modifyGetModelFromBlockState(
    //~ if > 1.21.11 'blockRendererDispatcher:' -> 'modelSet:'
    //~ if > 1.21.11 'BlockRenderDispatcher' -> 'BlockStateModelSet'
    modelSet: BlockStateModelSet,
    state: BlockState?,
    cir: CallbackInfoReturnable<BlockStateModel>,
) {
    if (!SkyBlockUtils.inSkyBlock) return
    val returnState = MiningCommissionsBlocksColor.processState(state) ?: return
    if (returnState != state) {
        //~ if > 1.21.11 'blockRendererDispatcher.blockModelShaper.getBlockModel' -> 'modelSet.get'
        cir.returnValue = modelSet.get(returnState)
    }
}
