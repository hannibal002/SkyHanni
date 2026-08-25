package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.state.level.CameraRenderState

//? if < 26.2 {
/*import net.minecraft.client.renderer.MultiBufferSource
*///?}

@PrimaryFunction("onRenderWorld")
class SkyHanniRenderWorldEvent(
    val matrices: PoseStack,
    val camera: CameraRenderState,
    val submitNodeCollector: SubmitNodeCollector,
    //? if < 26.2
    //val bufferSource: MultiBufferSource.BufferSource,
    val partialTicks: Float,
) : SkyHanniEvent() {
    var isCurrentlyDeferring: Boolean = true
}
