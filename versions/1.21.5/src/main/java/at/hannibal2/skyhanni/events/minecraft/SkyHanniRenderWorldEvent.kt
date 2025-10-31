package at.hannibal2.hanni.events.minecraft

import at.hannibal2.hanni.api.event.HanniEvent
import net.minecraft.client.render.Camera
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.math.MatrixStack

class HanniRenderWorldEvent(
    val matrices: MatrixStack,
    val camera: Camera,
    val vertexConsumers: VertexConsumerProvider.Immediate,
    val partialTicks: Float,
    var isCurrentlyDeferring: Boolean = true,
) : HanniEvent()
