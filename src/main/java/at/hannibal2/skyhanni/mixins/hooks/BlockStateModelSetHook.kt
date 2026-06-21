package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.features.mining.MiningCommissionsBlocksColor
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import net.minecraft.world.level.block.state.BlockState

//? if >= 26.1 {
import net.minecraft.client.renderer.block.BlockStateModelSet
import net.minecraft.client.renderer.block.dispatch.BlockStateModel
//?} else {
/*import net.minecraft.client.renderer.block.BlockRenderDispatcher
import net.minecraft.client.renderer.block.model.BlockStateModel
*///?}

fun getModelOverride(
    //~ if < 26.1 'BlockStateModelSet' -> 'BlockRenderDispatcher'
    modelSet: BlockStateModelSet,
    state: BlockState?,
): BlockStateModel? {
    if (!SkyBlockUtils.inSkyBlock) return null

    return MiningCommissionsBlocksColor.processState(state).takeUnless { it == state }?.let { returnState ->
        //~ if < 26.1 'get' -> 'blockModelShaper.getBlockModel'
        modelSet.get(returnState)
    }
}
