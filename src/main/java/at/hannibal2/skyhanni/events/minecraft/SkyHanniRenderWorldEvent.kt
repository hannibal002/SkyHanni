package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource

//~ if < 26.1 '.renderer.state.level.CameraRenderState' -> '.Camera'
import net.minecraft.client.renderer.state.level.CameraRenderState

@PrimaryFunction("onRenderWorld")
class SkyHanniRenderWorldEvent(
    val matrices: PoseStack,
    //~ if < 26.1 'CameraRenderState' -> 'Camera'
    val camera: CameraRenderState,
    val vertexConsumers: MultiBufferSource.BufferSource,
    val partialTicks: Float,
    var isCurrentlyDeferring: Boolean = true,
) : SkyHanniEvent()
