package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.api.event.Thread
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.state.level.CameraRenderState

//? if >= 26.2 {
import net.minecraft.client.renderer.SubmitNodeStorage
//?} else {
/*import net.minecraft.client.renderer.MultiBufferSource
*///?}

@Thread(RENDER)
@PrimaryFunction("onRenderWorld")
class SkyHanniRenderWorldEvent(
    val matrices: PoseStack,
    val camera: CameraRenderState,
    //~ if < 26.2 'submitNodeStorage: SubmitNodeStorage' -> 'bufferSource: MultiBufferSource.BufferSource'
    val submitNodeStorage: SubmitNodeStorage,
    val partialTicks: Float,
    var isCurrentlyDeferring: Boolean = true,
) : SkyHanniEvent() {

    //? if >= 26.2
    internal var skyHanniTextSubmitOrder = 0
}
