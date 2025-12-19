package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.RenderCompat.createRenderPass
import at.hannibal2.skyhanni.utils.compat.RenderCompat.drawIndexed
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.BufferBuilder
import net.minecraft.client.render.BuiltBuffer
import net.minecraft.client.render.Tessellator
//#if MC < 1.21.6
import net.minecraft.client.util.math.MatrixStack
//#else
//$$ import org.joml.Matrix3x2f
//#endif

object RenderPipelineDrawer {
    //#if MC < 1.21.6
    val matrices: MatrixStack.Entry get() = DrawContextUtils.drawContext.matrices.peek()
    //#else
    //$$ val matrices: Matrix3x2f get() = Matrix3x2f(DrawContextUtils.drawContext.matrices)
    //#endif
    fun getBuffer(
        pipeline: RenderPipeline,
    ): BufferBuilder = Tessellator.getInstance().begin(pipeline.vertexFormatMode, pipeline.vertexFormat)

    /**
     * Method inspired by SkyOcean's [InventoryRenderer](https://github.com/meowdding/SkyOcean/blob/main/src/client/kotlin/me/owdding/skyocean/utils/rendering/InventoryRenderer.kt)
     */
    fun draw(pipeline: RenderPipeline, mesh: BuiltBuffer, pass: (RenderPass) -> Unit) {
        val vertexBuffer = pipeline.vertexFormat.uploadImmediateVertexBuffer(mesh.buffer)

        val sequentialBuffer = RenderSystem.getSequentialBuffer(mesh.drawParameters.mode)
        val indexBuffer = sequentialBuffer.getIndexBuffer(mesh.drawParameters.indexCount)
        val indexType = sequentialBuffer.indexType

        val framebuffer = MinecraftClient.getInstance().framebuffer

        RenderSystem.getDevice().createRenderPass(
            "SkyHanni Immediate Pipeline Draw",
            framebuffer,
        ).use { renderPass ->
            pass.invoke(renderPass)

            renderPass.setPipeline(pipeline)
            renderPass.setVertexBuffer(0, vertexBuffer)
            renderPass.setIndexBuffer(indexBuffer, indexType)

            renderPass.drawIndexed(mesh.drawParameters.indexCount)
        }

        mesh.close()
    }
}
