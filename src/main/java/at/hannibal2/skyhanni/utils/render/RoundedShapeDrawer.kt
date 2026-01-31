package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.shader.CircleShader
import at.hannibal2.skyhanni.shader.RadialGradientCircleShader
import at.hannibal2.skyhanni.shader.RoundedRectangleOutlineShader
import at.hannibal2.skyhanni.shader.RoundedRectangleShader
import at.hannibal2.skyhanni.shader.RoundedShader
import at.hannibal2.skyhanni.shader.RoundedTextureShader
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import io.github.notenoughupdates.moulconfig.ChromaColour
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import at.hannibal2.skyhanni.utils.render.uniforms.SkyHanniCircleUniform
import at.hannibal2.skyhanni.utils.render.uniforms.SkyHanniRadialGradientCircleUniform
import at.hannibal2.skyhanni.utils.render.uniforms.SkyHanniRoundedOutlineUniform
import at.hannibal2.skyhanni.utils.render.uniforms.SkyHanniRoundedUniform
import com.mojang.blaze3d.buffers.GpuBufferSlice
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer
import com.mojang.blaze3d.ProjectionType
import org.joml.Matrix4f
import org.joml.Vector4f
import org.joml.Vector3f
//? > 1.21.10
//import com.mojang.blaze3d.textures.FilterMode

object RoundedShapeDrawer {

    val projectionMatrix = CachedOrthoProjectionMatrixBuffer("SkyHanni Rounded Shapes", 1000.0f, 11000.0f, true)
    var roundedUniform = SkyHanniRoundedUniform()
    var roundedOutlineUniform = SkyHanniRoundedOutlineUniform()
    var circleUniform = SkyHanniCircleUniform()
    var radialGradientCircleUniform = SkyHanniRadialGradientCircleUniform()
    var roundedBufferSlice: GpuBufferSlice? = null
    var roundedOutlineBufferSlice: GpuBufferSlice? = null
    var circleBufferSlice: GpuBufferSlice? = null
    var radialGradientCircleBufferSlice: GpuBufferSlice? = null

    private fun <T : RoundedShader<T>> T.performBaseUniforms(
        renderPass: RenderPass,
    ) {
        renderPass.setUniform("SkyHanniRoundedUniforms", roundedBufferSlice)
    }

    private fun <T : RoundedShader<T>> T.performVQuadAndUniforms(
        pipeline: RenderPipeline,
        x1: Int, y1: Int, x2: Int, y2: Int,
        postVertexOps: List<(BufferBuilder.() -> Unit)>,
        prePassOp: (() -> Unit) = {},
        passOp: (RenderPass.() -> Unit) = { },
    ) {
        val floatPairs = listOf(
            x1 to y1,
            x1 to y2,
            x2 to y2,
            x2 to y1,
        ).map { (x, y) -> x.toFloat() to y.toFloat() }

        with(RenderPipelineDrawer) {
            val buffer = getBuffer(pipeline)
            floatPairs.forEachIndexed { i, (x, y) ->
                buffer.addVertexWith2DPose(matrices, x, y).apply {
                    val postOp = postVertexOps.getOrNull(i) ?: postVertexOps.getOrNull(0) ?: return@forEachIndexed
                    postOp.invoke(buffer)
                }
            }

            // Need to backup current projection matrix and set current to an orthographic
            // projection matrix, since orthographic gui elements in 1.21.7 are now deferred
            // so we just set the correct matrix here are restore the perspective one afterwards
            val window = Minecraft.getInstance().window
            RenderSystem.backupProjectionMatrix()
            RenderSystem.setProjectionMatrix(
                projectionMatrix.getBuffer(
                    window.width.toFloat() / window.guiScale.toFloat(),
                    window.height.toFloat() / window.guiScale.toFloat(),
                ),
                ProjectionType.ORTHOGRAPHIC,
            )
            val dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(
                Matrix4f().setTranslation(0.0f, 0.0f, -11000.0f),
                Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
                Vector3f(),
                //? if < 1.21.11 {
                RenderSystem.getTextureMatrix(),
                RenderSystem.getShaderLineWidth(),
                //?} else
                //Matrix4f(),
            )
            roundedBufferSlice =
                roundedUniform.writeWith(scaleFactor, radius, smoothness, halfSize, centerPos, modelViewMatrix)
            prePassOp.invoke()

            draw(pipeline, buffer.buildOrThrow()) { pass ->
                RenderSystem.bindDefaultUniforms(pass)
                pass.setUniform("DynamicTransforms", dynamicTransforms)
                this@performVQuadAndUniforms.performBaseUniforms(pass)
                passOp.invoke(pass)
            }

            RenderSystem.restoreProjectionMatrix()
        }
    }

    fun drawRoundedRect(left: Int, top: Int, right: Int, bottom: Int, color: Int) =
        RoundedRectangleShader.performVQuadAndUniforms(
            SkyHanniRenderPipeline.ROUNDED_RECT(),
            x1 = left, y1 = top, x2 = right, y2 = bottom,
            postVertexOps = listOf { setColor(color) },
        )

    fun drawRoundedTexturedRect(left: Int, top: Int, right: Int, bottom: Int, texture: Identifier) {
        val glTex = Minecraft.getInstance().textureManager.getTexture(texture).textureView
        RenderSystem.assertOnRenderThread()
        //? if < 1.21.11
        RenderSystem.setShaderTexture(0, glTex)
        RoundedTextureShader.performVQuadAndUniforms(
            SkyHanniRenderPipeline.ROUNDED_TEXTURED_RECT(),
            x1 = left, y1 = top, x2 = right, y2 = bottom,
            postVertexOps = listOf(
                { setUv(0f, 0f) },
                { setUv(0f, 1f) },
                { setUv(1f, 1f) },
                { setUv(1f, 0f) },
            ),
        ) {
            //? if < 1.21.11 {
            bindSampler("textureSampler", glTex)
            //?} else {
            /*val sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST)
            bindTexture("textureSampler", glTex, sampler)
            *///?}
        }
    }


    fun drawRoundedRectOutline(left: Int, top: Int, right: Int, bottom: Int, topColor: Int, bottomColor: Int) =
        RoundedRectangleOutlineShader.performVQuadAndUniforms(
            SkyHanniRenderPipeline.ROUNDED_RECT_OUTLINE(),
            x1 = left, y1 = top, x2 = right, y2 = bottom,
            postVertexOps = listOf(
                { setColor(topColor) },
                { setColor(bottomColor) },
                { setColor(bottomColor) },
                { setColor(topColor) },
            ),
            {
                roundedOutlineBufferSlice = roundedOutlineUniform.writeWith(
                    RoundedRectangleOutlineShader.borderThickness, RoundedRectangleOutlineShader.borderBlur,
                )
            },
        ) {
            setUniform("SkyHanniRoundedOutlineUniforms", roundedOutlineBufferSlice)
        }

    fun drawRoundedRect(left: Int, top: Int, right: Int, bottom: Int, topColor: Int, bottomColor: Int) =
        RoundedRectangleShader.performVQuadAndUniforms(
            SkyHanniRenderPipeline.ROUNDED_RECT(),
            x1 = left, y1 = top, x2 = right, y2 = bottom,
            postVertexOps = listOf(
                { setColor(topColor) },
                { setColor(bottomColor) },
                { setColor(bottomColor) },
                { setColor(topColor) },
            ),
        )

    fun drawCircle(left: Int, top: Int, right: Int, bottom: Int, color: Int) = CircleShader.performVQuadAndUniforms(
        SkyHanniRenderPipeline.CIRCLE(),
        x1 = left, y1 = top, x2 = right, y2 = bottom,
        postVertexOps = listOf { setColor(color) },
        {
            circleBufferSlice = circleUniform.writeWith(
                CircleShader.angle1, CircleShader.angle2,
            )
        },
    ) {
        setUniform("SkyHanniCircleUniforms", circleBufferSlice)
    }

    fun drawGradientCircle(
        left: Int, top: Int, right: Int, bottom: Int, startColor: ChromaColour, endColor: ChromaColour,
    ) = RadialGradientCircleShader.performVQuadAndUniforms(
        SkyHanniRenderPipeline.RADIAL_GRADIENT_CIRCLE(),
        x1 = left, y1 = top, x2 = right, y2 = bottom,
        postVertexOps = listOf(
            { setColor(startColor.toColor().rgb) },
            { setColor(endColor.toColor().rgb) },
        ),
        {
            radialGradientCircleBufferSlice = radialGradientCircleUniform.writeWith(
                RadialGradientCircleShader.angle,
                Vector4f(startColor.destructToFloatArray()),
                Vector4f(endColor.destructToFloatArray()),
                RadialGradientCircleShader.progress,
                RadialGradientCircleShader.phaseOffset,
                RadialGradientCircleShader.reverse,
            )
        },
    ) {
        setUniform("SkyHanniRadialGradientCircleUniforms", radialGradientCircleBufferSlice)
    }

    private fun ChromaColour.destructToFloatArray(): FloatArray {
        return floatArrayOf(
            this.toColor().red.toFloat() / 255f,
            this.toColor().green.toFloat() / 255f,
            this.toColor().blue.toFloat() / 255f,
            this.alpha.toFloat() / 255f,
        )
    }

    fun clearUniforms() {
        roundedUniform.clear()
        roundedOutlineUniform.clear()
        circleUniform.clear()
    }
}
