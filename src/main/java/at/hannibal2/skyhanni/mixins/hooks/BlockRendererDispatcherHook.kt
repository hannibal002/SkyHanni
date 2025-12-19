package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.features.mining.MiningCommissionsBlocksColor
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import net.minecraft.block.BlockState
import net.minecraft.client.render.block.BlockRenderManager
import net.minecraft.client.render.model.BlockStateModel
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

fun modifyGetModelFromBlockState(
    blockRendererDispatcher: BlockRenderManager,
    state: BlockState?,
    cir: CallbackInfoReturnable<BlockStateModel>,
) {
    if (!SkyBlockUtils.inSkyBlock) return

    val returnState = MiningCommissionsBlocksColor.processState(state)

    if (returnState != state) {
        cir.returnValue = blockRendererDispatcher.models.getModel(returnState)
    }
}
