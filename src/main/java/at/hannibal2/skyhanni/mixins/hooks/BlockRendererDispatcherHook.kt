package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.features.mining.MiningCommissionsBlocksColor
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import net.minecraft.world.level.block.state.BlockState
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

//? if >= 26.1 {
import net.minecraft.client.renderer.block.BlockStateModelSet
import net.minecraft.client.renderer.block.dispatch.BlockStateModel
//?} else {
/*import net.minecraft.client.renderer.block.BlockRenderDispatcher
import net.minecraft.client.renderer.block.model.BlockStateModel
*///?}

fun modifyGetModelFromBlockState(
    //? if >= 26.1 {
    modelSet: BlockStateModelSet,
    //?} else {
    /*blockRenderDispatcher: BlockRenderDispatcher,
    *///?}
    state: BlockState?,
    cir: CallbackInfoReturnable<BlockStateModel>,
) {
    if (!SkyBlockUtils.inSkyBlock) return
    val returnState = MiningCommissionsBlocksColor.processState(state) ?: return

    if (returnState != state) {
        cir.returnValue =
            //? if >= 26.1 {
            modelSet.get(returnState)
        //?} else {
        /*blockRenderDispatcher.blockModelShaper.getBlockModel(returnState)
        *///?}
    }
}
