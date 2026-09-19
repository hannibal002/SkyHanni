package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.features.mining.MiningCommissionsBlocksColor
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import net.minecraft.client.renderer.block.BlockStateModelSet
import net.minecraft.client.renderer.block.dispatch.BlockStateModel
import net.minecraft.world.level.block.state.BlockState
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

fun modifyGetModelFromBlockState(
    modelSet: BlockStateModelSet,
    state: BlockState?,
    cir: CallbackInfoReturnable<BlockStateModel>,
) {
    if (!SkyBlockUtils.inSkyBlock) return
    val returnState = MiningCommissionsBlocksColor.processState(state) ?: return

    if (returnState != state) {
        cir.returnValue =
            modelSet.get(returnState)
    }
}
