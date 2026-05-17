package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.features.mining.MiningCommissionsBlocksColor
import at.hannibal2.skyhanni.utils.SkyBlockUtils
//~ if < 26.1 'BlockStateModelSet' -> 'BlockRenderDispatcher'
import net.minecraft.client.renderer.block.BlockStateModelSet
//~ if < 26.1 'dispatch.BlockStateModel' -> 'model.BlockStateModel'
import net.minecraft.client.renderer.block.dispatch.BlockStateModel
import net.minecraft.world.level.block.state.BlockState
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

fun modifyGetModelFromBlockState(
    //~ if < 26.1 'modelSet:' -> 'blockRendererDispatcher:'
    //~ if < 26.1 'BlockStateModelSet' -> 'BlockRenderDispatcher'
    modelSet: BlockStateModelSet,
    state: BlockState?,
    cir: CallbackInfoReturnable<BlockStateModel>,
) {
    if (!SkyBlockUtils.inSkyBlock) return
    val returnState = MiningCommissionsBlocksColor.processState(state) ?: return
    if (returnState != state) {
        //~ if < 26.1 'modelSet.get' -> 'blockRendererDispatcher.blockModelShaper.getBlockModel'
        cir.returnValue = modelSet.get(returnState)
    }
}
