package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Camera
import net.minecraft.client.renderer.MultiBufferSource

class SkyHanniRenderWorldEvent(
    val matrices: PoseStack,
    val camera: Camera,
    val vertexConsumers: MultiBufferSource.BufferSource,
    val partialTicks: Float,
    var isCurrentlyDeferring: Boolean = true,
) : SkyHanniEvent()
