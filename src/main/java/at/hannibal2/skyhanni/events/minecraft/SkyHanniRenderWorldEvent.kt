package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.state.level.CameraRenderState

//? if >= 26.2 {
import net.minecraft.client.renderer.SubmitNodeCollector
//?} else {
/*import net.minecraft.client.renderer.MultiBufferSource
*///?}

@PrimaryFunction("onRenderWorld")
class SkyHanniRenderWorldEvent(
    val matrices: PoseStack,
    val camera: CameraRenderState,
    //~ if < 26.2 'submitNodeStorage: SubmitNodeCollector' -> 'bufferSource: MultiBufferSource.BufferSource'
    val submitNodeStorage: SubmitNodeCollector,
    val partialTicks: Float,
) : SkyHanniEvent() {
    var isCurrentlyDeferring: Boolean = true
}
