//~ if < 26.1 'BlockStateModelSet' -> 'BlockRenderDispatcher' {
package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.features.mining.MiningCommissionsBlocksColor
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import net.minecraft.client.renderer.block.BlockStateModelSet
import net.minecraft.world.level.block.state.BlockState
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

//~ if < 26.1 'dispatch.BlockStateModel' -> 'model.BlockStateModel'
import net.minecraft.client.renderer.block.dispatch.BlockStateModel

fun modifyGetModelFromBlockState(
    modelSet: BlockStateModelSet,
    state: BlockState?,
    cir: CallbackInfoReturnable<BlockStateModel>,
) {
    if (!SkyBlockUtils.inSkyBlock) return
    val returnState = MiningCommissionsBlocksColor.processState(state) ?: return
    if (returnState != state) {
        //~ if < 26.1 'get' -> 'blockModelShaper.getBlockModel'
        cir.returnValue = modelSet.get(returnState)
    }
}
//~}
