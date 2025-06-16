package at.hannibal2.skyhanni.utils.render.layers

import at.hannibal2.skyhanni.api.minecraftevents.ClientEvents
import at.hannibal2.skyhanni.config.features.chroma.ChromaConfig.Direction
import at.hannibal2.skyhanni.features.chroma.ChromaManager
import at.hannibal2.skyhanni.mixins.transformers.AccessorMinecraft
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.VertexFormat
import java.util.OptionalDouble
import java.util.OptionalInt
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.BuiltBuffer
import net.minecraft.client.render.RenderLayer.MultiPhase

class ChromaRenderLayer(
    name: String, size: Int, hasCrumbling: Boolean, translucent: Boolean, pipeline: RenderPipeline, phases: MultiPhaseParameters,
) : MultiPhase(name, size, hasCrumbling, translucent, pipeline, phases ) {

    override fun draw(buffer: BuiltBuffer) {
        val renderPipeline = this.pipeline
        this.startDrawing()

        try {
            val gpuBuffer = renderPipeline.vertexFormat.uploadImmediateVertexBuffer(buffer.buffer)
            val gpuBuffer2: GpuBuffer
            val indexType: VertexFormat.IndexType
            if (buffer.sortedBuffer == null) {
                val shapeIndexBuffer = RenderSystem.getSequentialBuffer(buffer.drawParameters.mode())
                gpuBuffer2 = shapeIndexBuffer.getIndexBuffer(buffer.drawParameters.indexCount())
                indexType = shapeIndexBuffer.indexType
            } else {
                gpuBuffer2 = renderPipeline.vertexFormat.uploadImmediateIndexBuffer(buffer.sortedBuffer)
                indexType = buffer.drawParameters.indexType()
            }

            val framebuffer = phases.target.get()
            val colorAttachment = framebuffer.colorAttachment
            val depthAttachment = if (framebuffer.useDepthAttachment) framebuffer.depthAttachment else null

            RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                colorAttachment, OptionalInt.empty(),
                depthAttachment, OptionalDouble.empty()
            ).use { renderPass ->
                // Set custom chroma uniforms
                val chromaSize: Float = ChromaManager.config.chromaSize * (GuiScreenUtils.displayWidth / 100f)
                var ticks = (ClientEvents.totalTicks) + (MinecraftClient.getInstance() as AccessorMinecraft).timer.getTickProgress(true)
                ticks = when (ChromaManager.config.chromaDirection) {
                    Direction.FORWARD_RIGHT, Direction.BACKWARD_RIGHT -> ticks
                    Direction.FORWARD_LEFT, Direction.BACKWARD_LEFT -> -ticks
                }
                val timeOffset: Float = ticks * (ChromaManager.config.chromaSpeed / 360f)
                val saturation: Float = ChromaManager.config.chromaSaturation
                val forwardDirection: Int = when (ChromaManager.config.chromaDirection) {
                    Direction.FORWARD_RIGHT, Direction.FORWARD_LEFT -> 1
                    Direction.BACKWARD_RIGHT, Direction.BACKWARD_LEFT -> 0
                }

                renderPass.setUniform("chromaSize", chromaSize)
                renderPass.setUniform("timeOffset", timeOffset)
                renderPass.setUniform("saturation", saturation)
                renderPass.setUniform("forwardDirection", forwardDirection)

                renderPass.setPipeline(renderPipeline)
                renderPass.setVertexBuffer(0, gpuBuffer)

                if (RenderSystem.SCISSOR_STATE.isEnabled) {
                    renderPass.enableScissor(RenderSystem.SCISSOR_STATE)
                }

                for (i in 0..11) {
                    val gpuTexture = RenderSystem.getShaderTexture(i)
                    if (gpuTexture != null) {
                        renderPass.bindSampler("Sampler$i", gpuTexture)
                    }
                }

                renderPass.setIndexBuffer(gpuBuffer2, indexType)
                renderPass.drawIndexed(0, buffer.drawParameters.indexCount())
            }
        } catch (exception: Throwable) {
            try {
                buffer.close()
            } catch (exception2: Throwable) {
                exception.addSuppressed(exception2)
            }

            throw exception
        }

        buffer.close()
        this.endDrawing()
    }

}
