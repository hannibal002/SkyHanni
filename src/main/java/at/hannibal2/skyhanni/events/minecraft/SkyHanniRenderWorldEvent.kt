package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.state.level.CameraRenderState

//? if < 26.2 {
/*import net.minecraft.client.renderer.MultiBufferSource
*///?}

/**
 * Posted at Fabric API's `LevelRenderEvents.COLLECT_SUBMITS`. This is the preferred render event.
 * It provides callers with a [SubmitNodeCollector] that can be used to submit renderable elements.
 */
@PrimaryFunction("onRenderWorld")
class SkyHanniRenderWorldEvent(
    val matrices: PoseStack,
    val camera: CameraRenderState,
    val submitNodeCollector: SubmitNodeCollector,
    val partialTicks: Float,
) : SkyHanniEvent() {
    var isCurrentlyDeferring: Boolean = true
}

//? if < 26.2 {
/*/**
 * Posted at Fabric API's `LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN`. This should only be used
 * in cases where the [SubmitNodeCollector]-based API does not provide the necessary functionality.
 * It provides callers with a [MultiBufferSource.BufferSource] that can be used to render elements.
 */
@PrimaryFunction("onRenderWorldLegacy")
class SkyHanniRenderWorldEventLegacy(
    val bufferSource: MultiBufferSource.BufferSource,
) : SkyHanniEvent()
*///?}
