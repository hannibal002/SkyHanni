package at.hannibal2.skyhanni.utils.render.layers

import at.hannibal2.skyhanni.mixins.hooks.GuiRendererHook
import at.hannibal2.skyhanni.utils.compat.RenderCompat.createRenderPass
import at.hannibal2.skyhanni.utils.compat.RenderCompat.drawIndexed
import at.hannibal2.skyhanni.utils.compat.RenderCompat.enableRenderPassScissorStateIfAble
import at.hannibal2.skyhanni.utils.render.SkyHanniRenderPipeline
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.MeshData
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.resources.Identifier
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f

class ChromaRenderLayer(
    name: String,
    texture: Identifier? = null,
) : RenderType(
    name,
    if (texture == null) {
        RenderSetup.builder(SkyHanniRenderPipeline.CHROMA_STANDARD())
    } else {
        RenderSetup.builder(SkyHanniRenderPipeline.CHROMA_TEXT()).withTexture("texture", texture)
    }.createRenderSetup(),
) {

    override fun draw(buffer: MeshData) {
        val renderPipeline = this.state.pipeline
        val matrix4fStack = RenderSystem.getModelViewStack()
        val consumer = this.state.layeringTransform.modifier
        if (consumer != null) {
            matrix4fStack.pushMatrix()
            consumer.accept(matrix4fStack)
        }

        val dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(
            RenderSystem.getModelViewMatrix(), Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
            Vector3f(),
            Matrix4f(),
        )
        if (GuiRendererHook.chromaBufferSlice == null) {
            GuiRendererHook.computeChromaBufferSlice()
        }

        try {
            val gpuBuffer = renderPipeline.vertexFormat.uploadImmediateVertexBuffer(buffer.vertexBuffer())
            val gpuBuffer2: GpuBuffer
            val indexType: VertexFormat.IndexType
            val indexBuffer = buffer.indexBuffer()
            if (indexBuffer == null) {
                val shapeIndexBuffer = RenderSystem.getSequentialBuffer(buffer.drawState().mode())
                gpuBuffer2 = shapeIndexBuffer.getBuffer(buffer.drawState().indexCount())
                indexType = shapeIndexBuffer.type()
            } else {
                gpuBuffer2 = renderPipeline.vertexFormat.uploadImmediateIndexBuffer(indexBuffer)
                indexType = buffer.drawState().indexType()
            }

            val framebuffer = state.outputTarget.renderTarget

            RenderSystem.getDevice().createRenderPass("SkyHanni Immediate Chroma Pipeline Draw", framebuffer)
                .use { renderPass ->
                    RenderSystem.bindDefaultUniforms(renderPass)
                    renderPass.setUniform("DynamicTransforms", dynamicTransforms)
                    GuiRendererHook.chromaBufferSlice?.let {
                        renderPass.setUniform("SkyHanniChromaUniforms", it)
                    }

                    renderPass.setPipeline(renderPipeline)
                    renderPass.setVertexBuffer(0, gpuBuffer)

                    renderPass.enableRenderPassScissorStateIfAble()

                    for (entry in this.state.getTextures()) {
                        renderPass.bindTexture(entry.key, entry.value.textureView, entry.value.sampler)
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
        if (consumer != null) {
            matrix4fStack.popMatrix()
        }
    }

}
