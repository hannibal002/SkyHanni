package at.hannibal2.skyhanni.utils.render.layers

import at.hannibal2.skyhanni.mixins.hooks.GuiRendererHook
import at.hannibal2.skyhanni.utils.compat.RenderCompat.createRenderPass
import at.hannibal2.skyhanni.utils.compat.RenderCompat.drawIndexed
import at.hannibal2.skyhanni.utils.compat.RenderCompat.enableRenderPassScissorStateIfAble
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.MeshData
import com.mojang.blaze3d.vertex.VertexFormat
import org.joml.Vector3f
import org.joml.Vector4f
//? if < 1.21.11 {
import net.minecraft.client.renderer.rendertype.RenderType.CompositeRenderType
//?} else {
/*import net.minecraft.resources.Identifier
import org.joml.Matrix4f
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.rendertype.RenderSetup
import at.hannibal2.skyhanni.utils.render.SkyHanniRenderPipeline
*///?}

class ChromaRenderLayer(
    name: String,
    size: Int,
    hasCrumbling: Boolean,
    translucent: Boolean,
    pipeline: RenderPipeline,
    //? if < 1.21.11 {
    phases: CompositeState,
) : CompositeRenderType(name, size, hasCrumbling, translucent, pipeline, phases) {
    //?} else {
    /*texture: Identifier? = null,
) : RenderType(
    name,
    if (texture == null) {
        RenderSetup.builder(SkyHanniRenderPipeline.CHROMA_STANDARD())
    } else {
        RenderSetup.builder(SkyHanniRenderPipeline.CHROMA_TEXT()).withTexture("texture", texture)
    }
        .createRenderSetup(),
) {
    *///?}

    override fun draw(buffer: MeshData) {

        //? if < 1.21.11 {
        val renderPipeline = this.renderPipeline
        this.setupRenderState()
        //?} else {
        /*val renderPipeline = this.state.pipeline
        val matrix4fStack = RenderSystem.getModelViewStack()
        val consumer = this.state.layeringTransform.modifier
        if (consumer != null) {
            matrix4fStack.pushMatrix()
            consumer.accept(matrix4fStack)
        }
        *///?}

        val dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(
            RenderSystem.getModelViewMatrix(), Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
            Vector3f(),
            //? if < 1.21.11 {
            RenderSystem.getTextureMatrix(),
            RenderSystem.getShaderLineWidth(),
            //?} else
            //Matrix4f(),
        )
        if (GuiRendererHook.chromaBufferSlice == null) {
            GuiRendererHook.computeChromaBufferSlice()
        }

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

            val framebuffer = state./*? if < 1.21.11 {*/ outputState /*?} else {*/ /*outputTarget *//*?}*/.renderTarget

            RenderSystem.getDevice().createRenderPass("SkyHanni Immediate Chroma Pipeline Draw", framebuffer)
                .use { renderPass ->
                    RenderSystem.bindDefaultUniforms(renderPass)
                    renderPass.setUniform("DynamicTransforms", dynamicTransforms)
                    renderPass.setUniform("SkyHanniChromaUniforms", GuiRendererHook.chromaBufferSlice)


                    renderPass.setPipeline(renderPipeline)
                    renderPass.setVertexBuffer(0, gpuBuffer)

                    renderPass.enableRenderPassScissorStateIfAble()

                    //? if < 1.21.11 {
                    for (i in 0..11) {
                        val gpuTexture = RenderSystem.getShaderTexture(i)
                        if (gpuTexture != null) {
                            renderPass.bindSampler("Sampler$i", gpuTexture)
                        }
                    }
                    //?} else {
                    /*for (entry in this.state.textures) {
                        renderPass.bindTexture(entry.key, entry.value.textureView, entry.value.sampler)
                    }
                    *///?}

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
        //? if < 1.21.11 {
        this.clearRenderState()
        //?} else {
        /*if (consumer != null) {
            matrix4fStack.popMatrix()
        }
        *///?}
    }

}
