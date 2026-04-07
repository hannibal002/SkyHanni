package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
//~ if > 1.21.11 '.Camera' -> '.renderer.state.level.CameraRenderState'
import net.minecraft.client.Camera

@PrimaryFunction("onRenderWorld")
class SkyHanniRenderWorldEvent(
    val matrices: PoseStack,
    //~ if > 1.21.11 'Camera' -> 'CameraRenderState'
    val camera: Camera,
    val vertexConsumers: MultiBufferSource.BufferSource,
    val partialTicks: Float,
    var isCurrentlyDeferring: Boolean = true,
) : SkyHanniEvent()
