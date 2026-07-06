package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import com.mojang.blaze3d.vertex.PoseStack
//~ if < 26.1 'renderer.state.level.CameraRenderState' -> 'Camera'
import net.minecraft.client.renderer.state.level.CameraRenderState

//? if >= 26.2 {
import net.minecraft.client.renderer.SubmitNodeStorage
//? } else {
/*import net.minecraft.client.renderer.MultiBufferSource
*///?}

@PrimaryFunction("onRenderWorld")
class SkyHanniRenderWorldEvent(
    val matrices: PoseStack,
    //~ if < 26.1 'CameraRenderState' -> 'Camera'
    val cameraState: CameraRenderState,
    //? if >= 26.2 {
    val submitNodeStorage: SubmitNodeStorage,
    //?} else {
    /*val bufferSource: MultiBufferSource.BufferSource,
    *///?}
    val partialTicks: Float,
    var isCurrentlyDeferring: Boolean = true,
) : SkyHanniEvent() {

    val camera get() = cameraState

    //? if >= 26.2
    internal var skyHanniTextSubmitOrder = 0
}
