package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.shader.RoundedRectangleOutlineShader
import at.hannibal2.skyhanni.shader.RoundedRectangleShader
import at.hannibal2.skyhanni.shader.RoundedShader
import at.hannibal2.skyhanni.shader.RoundedTextureShader
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderPass
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.BufferBuilder
import net.minecraft.util.Identifier

object RoundedShapeDrawer {

    private fun <T: RoundedShader<T>> T.performBaseUniforms(
        renderPass: RenderPass,
        withSmoothness: Boolean = true,
        withHalfSize: Boolean = true,
    ) {
        renderPass.setUniform("scaleFactor", this.scaleFactor)
        renderPass.setUniform("radius", this.radius)
        renderPass.setUniform("centerPos", this.centerPos[0], this.centerPos[1])
        renderPass.setUniform("modelViewMatrix", this.modelViewMatrix)
        if (withSmoothness) renderPass.setUniform("smoothness", this.smoothness)
        if (withHalfSize) renderPass.setUniform("halfSize", this.halfSize[0], this.halfSize[1])
    }

    private fun <T: RoundedShader<T>> T.performVQuadAndUniforms(
        pipeline: RenderPipeline,
        x1: Int, y1: Int, x2: Int, y2: Int,
        postVertexOps: List<(BufferBuilder.() -> Unit)>,
        withSmoothness: Boolean = true,
        withHalfSize: Boolean = true,
        passOp: (RenderPass.() -> Unit) = { },
    ) {
        val floatPairs = listOf(
            x1 to y1,
            x1 to y2,
            x2 to y2,
            x2 to y1
        ).map { (x, y) -> x.toFloat() to y.toFloat() }

        with(RenderPipelineDrawer) {
            val buffer = getBuffer(pipeline)
            floatPairs.forEachIndexed { i, (x, y) ->
                buffer.vertex(matrices, x, y, 0f).apply {
                    val postOp = postVertexOps.getOrNull(i)
                        ?: postVertexOps.getOrNull(0)
                        ?: return@forEachIndexed
                    postOp.invoke(buffer)
                }
            }

            draw(pipeline, buffer.end()) { pass ->
                this@performVQuadAndUniforms.performBaseUniforms(pass, withSmoothness, withHalfSize)
                passOp.invoke(pass)
            }
        }
    }

    fun drawRoundedRect(left: Int, top: Int, right: Int, bottom: Int, color: Int) =
        RoundedRectangleShader.performVQuadAndUniforms(
            SkyHanniRenderPipeline.ROUNDED_RECT(),
            x1 = left, y1 = top, x2 = right, y2 = bottom,
            postVertexOps = listOf { color(color) },
        )

    fun drawRoundedTexturedRect(left: Int, top: Int, right: Int, bottom: Int, texture: Identifier) =
        RoundedTextureShader.performVQuadAndUniforms(
            SkyHanniRenderPipeline.ROUNDED_TEXTURED_RECT(),
            x1 = left, y1 = top, x2 = right, y2 = bottom,
            postVertexOps = listOf(
                { texture(0f, 0f) },
                { texture(0f, 1f) },
                { texture(1f, 1f) },
                { texture(1f, 0f) },
            )
        ) {
            val glTexture = MinecraftClient.getInstance().textureManager.getTexture(texture).glTexture
            bindSampler("textureSampler", glTexture)
        }


    fun drawRoundedRectOutline(left: Int, top: Int, right: Int, bottom: Int, topColor: Int, bottomColor: Int) =
        RoundedRectangleOutlineShader.performVQuadAndUniforms(
            SkyHanniRenderPipeline.ROUNDED_RECT_OUTLINE(),
            x1 = left, y1 = top, x2 = right, y2 = bottom,
            postVertexOps = listOf(
                { color(topColor) },
                { color(bottomColor) },
                { color(bottomColor) },
                { color(topColor) },
            ),
            withSmoothness = false,
        ) {
            setUniform("borderThickness", RoundedRectangleOutlineShader.borderThickness)
            setUniform("borderBlur", RoundedRectangleOutlineShader.borderBlur)
        }

    fun drawRoundedRect(left: Int, top: Int, right: Int, bottom: Int, topColor: Int, bottomColor: Int) =
        RoundedRectangleShader.performVQuadAndUniforms(
            SkyHanniRenderPipeline.ROUNDED_RECT(),
            x1 = left, y1 = top, x2 = right, y2 = bottom,
            postVertexOps = listOf(
                { color(topColor) },
                { color(bottomColor) },
                { color(bottomColor) },
                { color(topColor) },
            ),
        )
}
