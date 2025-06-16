package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.features.misc.RoundedRectangleOutlineShader
import at.hannibal2.skyhanni.features.misc.RoundedRectangleShader
import at.hannibal2.skyhanni.features.misc.RoundedTextureShader
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.systems.RenderSystem
import java.util.OptionalDouble
import java.util.OptionalInt
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.BufferBuilder
import net.minecraft.client.render.BuiltBuffer
import net.minecraft.client.render.Tessellator
import net.minecraft.util.Identifier

object RoundedRectDrawer {

    fun drawRoundedRect(left: Int, top: Int, right: Int, bottom: Int, color: Int) {
        val pipeline = SkyHanniRenderPipelines.ROUNDED_RECT
        val matrices = DrawContextUtils.drawContext.matrices.peek()
        val buffer: BufferBuilder = Tessellator.getInstance().begin(pipeline.vertexFormatMode, pipeline.vertexFormat)
        buffer.vertex(matrices, left.toFloat(), top.toFloat(), 0f).color(color)
        buffer.vertex(matrices, left.toFloat(), bottom.toFloat(), 0f).color(color)
        buffer.vertex(matrices, right.toFloat(), bottom.toFloat(), 0f).color(color)
        buffer.vertex(matrices, right.toFloat(), top.toFloat(), 0f).color(color)

        this.draw(pipeline, buffer.end()) { pass ->
            pass.setUniform("scaleFactor", RoundedRectangleShader.scaleFactor)
            pass.setUniform("radius", RoundedRectangleShader.radius)
            pass.setUniform("smoothness", RoundedRectangleShader.smoothness)
            pass.setUniform("halfSize", RoundedRectangleShader.halfSize[0], RoundedRectangleShader.halfSize[1])
            pass.setUniform("centerPos", RoundedRectangleShader.centerPos[0], RoundedRectangleShader.centerPos[1])
            pass.setUniform("modelViewMatrix", RoundedRectangleShader.modelViewMatrix)
        }
    }

    fun drawRoundedTexturedRect(x: Int, y: Int, width: Int, height: Int, texture: Identifier) {
        val pipeline = SkyHanniRenderPipelines.ROUNDED_TEXTURED_RECT
        val matrices = DrawContextUtils.drawContext.matrices.peek()
        val buffer: BufferBuilder = Tessellator.getInstance().begin(pipeline.vertexFormatMode, pipeline.vertexFormat)
        buffer.vertex(matrices, x.toFloat(), y.toFloat(), 0f).texture(0f, 0f)
        buffer.vertex(matrices, x.toFloat(), height.toFloat(), 0f).texture(0f, 1f)
        buffer.vertex(matrices, width.toFloat(), height.toFloat(), 0f).texture(1f, 1f)
        buffer.vertex(matrices, width.toFloat(), y.toFloat(), 0f).texture(1f, 0f)

        val glTexture = MinecraftClient.getInstance().textureManager.getTexture(texture).glTexture

        this.draw(pipeline, buffer.end()) { pass ->
            pass.bindSampler("textureSampler", glTexture)
            pass.setUniform("scaleFactor", RoundedTextureShader.scaleFactor)
            pass.setUniform("radius", RoundedTextureShader.radius)
            pass.setUniform("smoothness", RoundedTextureShader.smoothness)
            pass.setUniform("halfSize", RoundedTextureShader.halfSize[0], RoundedTextureShader.halfSize[1])
            pass.setUniform("centerPos", RoundedTextureShader.centerPos[0], RoundedTextureShader.centerPos[1])
            pass.setUniform("modelViewMatrix", RoundedTextureShader.modelViewMatrix)
        }
    }

    fun drawRoundedRectOutline(left: Int, top: Int, right: Int, bottom: Int, topColor: Int, bottomColor: Int) {
        val pipeline = SkyHanniRenderPipelines.ROUNDED_RECT_OUTLINE
        val matrices = DrawContextUtils.drawContext.matrices.peek()
        val buffer: BufferBuilder = Tessellator.getInstance().begin(pipeline.vertexFormatMode, pipeline.vertexFormat)
        buffer.vertex(matrices, left.toFloat(), top.toFloat(), 0f).color(topColor)
        buffer.vertex(matrices, left.toFloat(), bottom.toFloat(), 0f).color(bottomColor)
        buffer.vertex(matrices, right.toFloat(), bottom.toFloat(), 0f).color(bottomColor)
        buffer.vertex(matrices, right.toFloat(), top.toFloat(), 0f).color(topColor)

        this.draw(pipeline, buffer.end()) { pass ->
            pass.setUniform("scaleFactor", RoundedRectangleOutlineShader.scaleFactor)
            pass.setUniform("radius", RoundedRectangleOutlineShader.radius)
            pass.setUniform("halfSize", RoundedRectangleOutlineShader.halfSize[0], RoundedRectangleOutlineShader.halfSize[1])
            pass.setUniform("centerPos", RoundedRectangleOutlineShader.centerPos[0], RoundedRectangleOutlineShader.centerPos[1])
            pass.setUniform("modelViewMatrix", RoundedRectangleOutlineShader.modelViewMatrix)
            pass.setUniform("borderThickness", RoundedRectangleOutlineShader.borderThickness)
            pass.setUniform("borderBlur", RoundedRectangleOutlineShader.borderBlur)
        }
    }

    /**
     * Method inspired by SkyOcean's [InventoryRenderer](https://github.com/meowdding/SkyOcean/blob/feat/iteam-search/src/client/kotlin/me/owdding/skyocean/utils/rendering/InventoryRenderer.kt)
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
