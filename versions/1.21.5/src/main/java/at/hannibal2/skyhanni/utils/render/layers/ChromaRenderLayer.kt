package at.hannibal2.skyhanni.utils.render.layers

import at.hannibal2.skyhanni.api.minecraftevents.ClientEvents
import at.hannibal2.skyhanni.config.features.chroma.ChromaConfig.Direction
import at.hannibal2.skyhanni.features.chroma.ChromaManager
import at.hannibal2.skyhanni.mixins.transformers.AccessorMinecraft
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import at.hannibal2.skyhanni.utils.compat.RenderCompat.createRenderPass
import at.hannibal2.skyhanni.utils.compat.RenderCompat.drawIndexed
import at.hannibal2.skyhanni.utils.compat.RenderCompat.enableRenderPassScissorStateIfAble
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.BuiltBuffer
import net.minecraft.client.render.RenderLayer.MultiPhase
//#if MC > 1.21.6
//$$ import at.hannibal2.skyhanni.mixins.hooks.GuiRendererHook
//$$ import org.joml.Vector4f
//#endif

class ChromaRenderLayer(
    name: String, size: Int, hasCrumbling: Boolean, translucent: Boolean, pipeline: RenderPipeline, phases: MultiPhaseParameters,
) : MultiPhase(name, size, hasCrumbling, translucent, pipeline, phases) {

    override fun draw(buffer: BuiltBuffer) {
        val renderPipeline = this.pipeline
        this.startDrawing()

        // Custom chroma uniforms
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

        //#if MC > 1.21.6
        //$$ var dynamicTransforms = RenderSystem.getDynamicUniforms()
        //$$     .write(
        //$$         RenderSystem.getModelViewMatrix(),
        //$$ 		 Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
        //$$ 		 RenderSystem.getModelOffset(),
        //$$ 		 RenderSystem.getTextureMatrix(),
        //$$ 		 RenderSystem.getShaderLineWidth()
        //$$     )
        //$$ if (GuiRendererHook.chromaBufferSlice == null) {
        //$$     GuiRendererHook.computeChromaBufferSlice()
        //$$ }
        //#endif

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

            RenderSystem.getDevice().createRenderPass("SkyHanni Immediate Chroma Pipeline Draw", framebuffer).use { renderPass ->
                //#if MC > 1.21.6
                //$$ RenderSystem.bindDefaultUniforms(renderPass)
                //$$ renderPass.setUniform("DynamicTransforms", dynamicTransforms)
                //$$ renderPass.setUniform("SkyHanniChromaUniforms", GuiRendererHook.chromaBufferSlice)
                //#else
                renderPass.setUniform("chromaSize", chromaSize)
                renderPass.setUniform("timeOffset", timeOffset)
                renderPass.setUniform("saturation", saturation)
                renderPass.setUniform("forwardDirection", forwardDirection)
                //#endif

                renderPass.setPipeline(renderPipeline)
                renderPass.setVertexBuffer(0, gpuBuffer)

                renderPass.enableRenderPassScissorStateIfAble()

                for (i in 0..11) {
                    val gpuTexture = RenderSystem.getShaderTexture(i)
                    if (gpuTexture != null) {
                        renderPass.bindSampler("Sampler$i", gpuTexture)
                    }
                }

                renderPass.setIndexBuffer(gpuBuffer2, indexType)
                renderPass.drawIndexed(buffer.drawParameters.indexCount())
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
