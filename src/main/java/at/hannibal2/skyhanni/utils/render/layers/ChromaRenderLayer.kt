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
import com.mojang.blaze3d.vertex.MeshData
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderType.CompositeRenderType

//? > 1.21.6 {
/*import at.hannibal2.skyhanni.mixins.hooks.GuiRendererHook
import org.joml.Vector4f
*///?}
//? > 1.21.8 {
/*import org.joml.Vector3f
 *///?}

class ChromaRenderLayer(
    name: String, size: Int, hasCrumbling: Boolean, translucent: Boolean, pipeline: RenderPipeline, phases: CompositeState,
) : CompositeRenderType(name, size, hasCrumbling, translucent, pipeline, phases) {

    override fun draw(buffer: MeshData) {
        val renderPipeline = this.renderPipeline
        this.setupRenderState()

        // Custom chroma uniforms
        val chromaSize: Float = ChromaManager.config.chromaSize * (GuiScreenUtils.displayWidth / 100f)
        var ticks = (ClientEvents.totalTicks) + (Minecraft.getInstance() as AccessorMinecraft).timer.getGameTimeDeltaPartialTick(true)
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

        //? > 1.21.6 {
        /*var dynamicTransforms = RenderSystem.getDynamicUniforms()
             .writeTransform(
                 RenderSystem.getModelViewMatrix(),
         		 Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
            //? < 1.21.9 {
                     RenderSystem.getModelOffset(),
            //?} else {
                     /*Vector3f(),
            *///?}
         		 RenderSystem.getTextureMatrix(),
         		 RenderSystem.getShaderLineWidth()
             )
         if (GuiRendererHook.chromaBufferSlice == null) {
             GuiRendererHook.computeChromaBufferSlice()
         }
        *///?}

        try {
            val gpuBuffer = renderPipeline.vertexFormat.uploadImmediateVertexBuffer(buffer.vertexBuffer())
            val gpuBuffer2: GpuBuffer
            val indexType: VertexFormat.IndexType
            if (buffer.indexBuffer() == null) {
                val shapeIndexBuffer = RenderSystem.getSequentialBuffer(buffer.drawState().mode())
                gpuBuffer2 = shapeIndexBuffer.getBuffer(buffer.drawState().indexCount())
                indexType = shapeIndexBuffer.type()
            } else {
                gpuBuffer2 = renderPipeline.vertexFormat.uploadImmediateIndexBuffer(buffer.indexBuffer())
                indexType = buffer.drawState().indexType()
            }

            val framebuffer = state.outputState.renderTarget

            RenderSystem.getDevice().createRenderPass("SkyHanni Immediate Chroma Pipeline Draw", framebuffer).use { renderPass ->
                //? > 1.21.6 {
                 /*RenderSystem.bindDefaultUniforms(renderPass)
                 renderPass.setUniform("DynamicTransforms", dynamicTransforms)
                 renderPass.setUniform("SkyHanniChromaUniforms", GuiRendererHook.chromaBufferSlice)
                *///?} else {
                renderPass.setUniform("chromaSize", chromaSize)
                renderPass.setUniform("timeOffset", timeOffset)
                renderPass.setUniform("saturation", saturation)
                renderPass.setUniform("forwardDirection", forwardDirection)
                //?}

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
                renderPass.drawIndexed(buffer.drawState().indexCount())
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
        this.clearRenderState()
    }

}
