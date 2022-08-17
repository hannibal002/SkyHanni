//package at.hannibal2.skyhanni.mixinhooks.render
//
//import at.hannibal2.skyhanni.events.RenderBlockInWorldEvent
//import net.minecraft.block.state.IBlockState
//import net.minecraft.client.renderer.BlockRendererDispatcher
//import net.minecraft.client.resources.model.IBakedModel
//import net.minecraft.util.BlockPos
//import net.minecraft.world.IBlockAccess
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
//
//fun modifyGetModelFromBlockState(
//    blockRendererDispatcher: Any,
//    state: IBlockState?,
//    worldIn: IBlockAccess,
//    pos: BlockPos?,
//    cir: CallbackInfoReturnable<IBakedModel>
//) {
//    (blockRendererDispatcher as BlockRendererDispatcher).apply {
//        val event = RenderBlockInWorldEvent(state, worldIn, pos)
//        event.postAndCatch()
//        if (event.state !== state) {
//            cir.returnValue = blockModelShapes.getModelForState(event.state)
//        }
//    }
//}