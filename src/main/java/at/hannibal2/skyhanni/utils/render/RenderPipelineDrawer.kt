package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.RenderCompat.createRenderPass
import at.hannibal2.skyhanni.utils.compat.RenderCompat.drawIndexed
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.MeshData
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import net.minecraft.client.Minecraft
//? > 1.21.6
//import org.joml.Matrix3x2f

object RenderPipelineDrawer {
    //? < 1.21.6 {
    val matrices: PoseStack.Pose get() = DrawContextUtils.drawContext.pose().last()
    //?} else {
    /*val matrices: Matrix3x2f get() = Matrix3x2f(DrawContextUtils.drawContext.pose())
    *///?}
    fun getBuffer(
        pipeline: RenderPipeline,
    ): BufferBuilder = Tesselator.getInstance().begin(pipeline.vertexFormatMode, pipeline.vertexFormat)

    /**
     * Method inspired by SkyOcean's [InventoryRenderer](https://github.com/meowdding/SkyOcean/blob/main/src/client/kotlin/me/owdding/skyocean/utils/rendering/InventoryRenderer.kt)
     */
    fun draw(pipeline: RenderPipeline, mesh: MeshData, pass: (RenderPass) -> Unit) {
        val vertexBuffer = pipeline.vertexFormat.uploadImmediateVertexBuffer(mesh.vertexBuffer())

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
    }
}
