package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.RenderCompat.createRenderPass
import at.hannibal2.skyhanni.utils.compat.RenderCompat.drawIndexed
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.MeshData
import net.minecraft.client.Minecraft
import org.joml.Matrix3x2f

//? if >= 26.2 {
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import net.minecraft.client.renderer.rendertype.RenderType
//?} else {
/*import com.mojang.blaze3d.vertex.Tesselator
*///?}

object RenderPipelineDrawer {

    val matrices: Matrix3x2f get() = Matrix3x2f(DrawContextUtils.drawContext.pose())

    fun getBuffer(pipeline: RenderPipeline): BufferBuilder {
        //? if >= 26.2 {
        return BufferBuilder(
            ByteBufferBuilder(RenderType.TRANSIENT_BUFFER_SIZE),
            pipeline.primitiveTopology,
            pipeline.getVertexFormatBinding(0) ?: error("Pipeline $pipeline has no vertex format binding 0"),
        )
        //?} else {
        /*return Tesselator.getInstance().begin(pipeline.vertexFormatMode, pipeline.vertexFormat)
        *///?}
    }

    /**
     * Method inspired by SkyOcean's [InventoryRenderer](https://github.com/meowdding/SkyOcean/blob/main/src/client/kotlin/me/owdding/skyocean/utils/rendering/InventoryRenderer.kt)
     */
    fun draw(pipeline: RenderPipeline, mesh: MeshData, pass: (RenderPass) -> Unit) {
        //? if >= 26.2 {
        val device = RenderSystem.getDevice()
        val vertexBuffer = device.createBuffer(
            { "SkyHanni immediate pipeline vertex buffer" },
            GpuBuffer.USAGE_VERTEX,
            mesh.vertexBuffer(),
        )

        val indexBuffer = mesh.indexBuffer()?.let {
            device.createBuffer({ "SkyHanni immediate pipeline index buffer" }, GpuBuffer.USAGE_INDEX, it)
        }

        val (indices, indexType) = indexBuffer?.let {
            it to mesh.drawState().indexType()
        } ?: run {
            val sequentialBuffer = RenderSystem.getSequentialBuffer(mesh.drawState().primitiveTopology())
            sequentialBuffer.getBuffer(mesh.drawState().indexCount()) to sequentialBuffer.type()
        }

        val framebuffer = Minecraft.getInstance().gameRenderer.mainRenderTarget()

        RenderSystem.getDevice().createRenderPass(
            "SkyHanni Immediate Pipeline Draw",
            framebuffer,
        ).use { renderPass ->
            pass.invoke(renderPass)

            renderPass.setPipeline(pipeline)
            renderPass.setVertexBuffer(0, vertexBuffer.slice())
            renderPass.setIndexBuffer(indices, indexType)

            renderPass.drawIndexed(mesh.drawState().indexCount())
        }

        indexBuffer?.close()
        vertexBuffer.close()
        mesh.close()
        //?} else {
        /*val vertexBuffer = pipeline.vertexFormat.uploadImmediateVertexBuffer(mesh.vertexBuffer())

        val sequentialBuffer = RenderSystem.getSequentialBuffer(mesh.drawState().mode)
        val indexBuffer = sequentialBuffer.getBuffer(mesh.drawState().indexCount)
        val indexType = sequentialBuffer.type()

        val framebuffer = Minecraft.getInstance().mainRenderTarget

        RenderSystem.getDevice().createRenderPass(
            "SkyHanni Immediate Pipeline Draw",
            framebuffer,
        ).use { renderPass ->
            pass.invoke(renderPass)

            renderPass.setPipeline(pipeline)
            renderPass.setVertexBuffer(0, vertexBuffer)
            renderPass.setIndexBuffer(indexBuffer, indexType)

            renderPass.drawIndexed(mesh.drawState().indexCount)
        }

        mesh.close()
        *///?}
    }
}
