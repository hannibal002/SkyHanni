package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.features.mining.MiningCommissionsBlocksColor
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import net.minecraft.client.renderer.block.BlockRenderDispatcher
import net.minecraft.client.renderer.block.model.BlockStateModel
import net.minecraft.world.level.block.state.BlockState
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

fun modifyGetModelFromBlockState(
    blockRendererDispatcher: BlockRenderDispatcher,
    state: BlockState?,
    cir: CallbackInfoReturnable<BlockStateModel>,
) {
    if (!SkyBlockUtils.inSkyBlock) return

    val returnState = MiningCommissionsBlocksColor.processState(state)

    if (returnState != state) {
        cir.returnValue = blockRendererDispatcher.blockModelShaper.getBlockModel(returnState)
    }
}
