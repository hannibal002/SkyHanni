package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.state.level.CameraRenderState

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
