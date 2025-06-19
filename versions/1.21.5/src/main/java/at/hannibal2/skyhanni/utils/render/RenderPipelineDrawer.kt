package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.BufferBuilder
import net.minecraft.client.render.BuiltBuffer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.util.math.MatrixStack
import java.util.OptionalDouble
import java.util.OptionalInt

object RenderPipelineDrawer {
    val matrices: MatrixStack.Entry get() = DrawContextUtils.drawContext.matrices.peek()
    fun getBuffer(
        pipeline: RenderPipeline,
    ): BufferBuilder = Tessellator.getInstance().begin(pipeline.vertexFormatMode, pipeline.vertexFormat)

    /**
     * Method inspired by SkyOcean's [InventoryRenderer](https://github.com/meowdding/SkyOcean/blob/main/src/client/kotlin/me/owdding/skyocean/utils/rendering/InventoryRenderer.kt)
     */
    fun draw(pipeline: RenderPipeline, mesh: BuiltBuffer, pass: (RenderPass) -> Unit) {
        val device = RenderSystem.getDevice()
        val vertexBuffer = pipeline.vertexFormat.uploadImmediateVertexBuffer(mesh.buffer)

        val sequentialBuffer = RenderSystem.getSequentialBuffer(mesh.drawParameters.mode)
        val indexBuffer = sequentialBuffer.getIndexBuffer(mesh.drawParameters.indexCount)
        val indexType = sequentialBuffer.indexType

        val framebuffer = MinecraftClient.getInstance().framebuffer
        val colorAttachment = framebuffer.colorAttachment
        val depthAttachment = if (framebuffer.useDepthAttachment) framebuffer.depthAttachment else null

        device.createCommandEncoder().createRenderPass(
            colorAttachment, OptionalInt.empty(),
            depthAttachment, OptionalDouble.empty()
        ).use { renderPass ->
            pass.invoke(renderPass)

            renderPass.setPipeline(pipeline)
            renderPass.setVertexBuffer(0, vertexBuffer)
            renderPass.setIndexBuffer(indexBuffer, indexType)

            renderPass.drawIndexed(0, mesh.drawParameters.indexCount)
        }

        mesh.close()
    }
}
