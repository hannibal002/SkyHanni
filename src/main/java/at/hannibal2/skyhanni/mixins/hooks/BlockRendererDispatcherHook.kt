package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.features.mining.MiningCommissionsBlocksColor
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import net.minecraft.client.renderer.block.BlockRenderDispatcher
import net.minecraft.client.renderer.block.model.BlockStateModel
import net.minecraft.world.level.block.state.BlockState

fun modifyGetModelFromBlockState(
    blockRendererDispatcher: BlockRenderDispatcher,
    state: BlockState?,
): BlockStateModel? {
    if (!SkyBlockUtils.inSkyBlock) return null

    val returnState = MiningCommissionsBlocksColor.processState(state)
    if (returnState != state) {
        return blockRendererDispatcher.blockModelShaper.getBlockModel(returnState)
    }
    return null
}
