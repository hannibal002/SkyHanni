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
import net.minecraft.resources.ResourceLocation
//#if MC > 1.21.6
//$$ import at.hannibal2.skyhanni.utils.render.uniforms.SkyHanniCircleUniform
//$$ import at.hannibal2.skyhanni.utils.render.uniforms.SkyHanniRadialGradientCircleUniform
//$$ import at.hannibal2.skyhanni.utils.render.uniforms.SkyHanniRoundedOutlineUniform
//$$ import at.hannibal2.skyhanni.utils.render.uniforms.SkyHanniRoundedUniform
//$$ import com.mojang.blaze3d.buffers.GpuBufferSlice
//$$ import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer
//$$ import com.mojang.blaze3d.ProjectionType
//$$ import org.joml.Matrix4f
//$$ import org.joml.Vector4f
//#endif
//#if MC > 1.21.8
//$$ import org.joml.Vector3f
//#endif

object RoundedShapeDrawer {

    //#if MC > 1.21.6
    //$$ val projectionMatrix = CachedOrthoProjectionMatrixBuffer("SkyHanni Rounded Shapes", 1000.0f, 11000.0f, true)
    //$$ var roundedUniform = SkyHanniRoundedUniform()
    //$$ var roundedOutlineUniform = SkyHanniRoundedOutlineUniform()
    //$$ var circleUniform = SkyHanniCircleUniform()
    //$$ var radialGradientCircleUniform = SkyHanniRadialGradientCircleUniform()
    //$$ var roundedBufferSlice: GpuBufferSlice? = null
    //$$ var roundedOutlineBufferSlice: GpuBufferSlice? = null
    //$$ var circleBufferSlice: GpuBufferSlice? = null
    //$$ var radialGradientCircleBufferSlice: GpuBufferSlice? = null
    //#endif

    private fun <T : RoundedShader<T>> T.performBaseUniforms(
        renderPass: RenderPass,
        withSmoothness: Boolean = true,
        withHalfSize: Boolean = true,
    ) {
        //#if MC < 1.21.6
        renderPass.setUniform("scaleFactor", this.scaleFactor)
        renderPass.setUniform("radius", this.radius)
        renderPass.setUniform("centerPos", this.centerPos[0], this.centerPos[1])
        renderPass.setUniform("modelViewMatrix", this.modelViewMatrix)
        if (withSmoothness) renderPass.setUniform("smoothness", this.smoothness)
        if (withHalfSize) renderPass.setUniform("halfSize", this.halfSize[0], this.halfSize[1])
        //#else
        //$$ renderPass.setUniform("SkyHanniRoundedUniforms", roundedBufferSlice)
        //#endif
    }

    private fun <T : RoundedShader<T>> T.performVQuadAndUniforms(
        pipeline: RenderPipeline,
        x1: Int, y1: Int, x2: Int, y2: Int,
        postVertexOps: List<(BufferBuilder.() -> Unit)>,
        //#if MC > 1.21.6
        //$$ prePassOp: (()-> Unit) = {},
        //#endif
        withSmoothness: Boolean = true,
        withHalfSize: Boolean = true,
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
                //#if MC < 1.21.6
                buffer.addVertex(matrices, x, y, 0f).apply {
                    //#elseif MC < 1.21.9
                    //$$ buffer.addVertexWith2DPose(matrices, x, y, 0f).apply {
                    //#else
                    //$$ buffer.addVertexWith2DPose(matrices, x, y).apply {
                    //#endif
                    val postOp = postVertexOps.getOrNull(i)
                        ?: postVertexOps.getOrNull(0)
                        ?: return@forEachIndexed
                    postOp.invoke(buffer)
                }
            }

            //#if MC > 1.21.6
            //$$ // Need to backup current projection matrix and set current to an orthographic
            //$$ // projection matrix, since orthographic gui elements in 1.21.7 are now deferred
            //$$ // so we just set the correct matrix here are restore the perspective one afterwards
            //$$ val window = Minecraft.getInstance().window
            //$$ RenderSystem.backupProjectionMatrix()
            //$$ RenderSystem.setProjectionMatrix(
            //$$     projectionMatrix.getBuffer(
            //$$         window.width.toFloat() / window.guiScale.toFloat(),
            //$$         window.height.toFloat() / window.guiScale.toFloat()),
            //$$     ProjectionType.ORTHOGRAPHIC
            //$$ )
            //$$ var dynamicTransforms = RenderSystem.getDynamicUniforms()
            //$$     .writeTransform(
            //$$         Matrix4f().setTranslation(0.0f, 0.0f, -11000.0f),
            //$$ 		 Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
            //#if MC < 1.21.9
            //$$ 		 RenderSystem.getModelOffset(),
            //#else
            //$$         Vector3f(),
            //#endif
            //$$ 		 RenderSystem.getTextureMatrix(),
            //$$ 		 RenderSystem.getShaderLineWidth()
            //$$     )
            //$$ roundedBufferSlice = roundedUniform.writeWith(scaleFactor, radius, smoothness, halfSize, centerPos, modelViewMatrix)
            //$$ prePassOp.invoke()
            //#endif

            draw(pipeline, buffer.buildOrThrow()) { pass ->
                //#if MC > 1.21.6
                //$$ RenderSystem.bindDefaultUniforms(pass)
                //$$ pass.setUniform("DynamicTransforms", dynamicTransforms)
                //#endif
                this@performVQuadAndUniforms.performBaseUniforms(pass, withSmoothness, withHalfSize)
                passOp.invoke(pass)
            }

            //#if MC > 1.21.6
            //$$ RenderSystem.restoreProjectionMatrix()
            //#endif
        }
    }

    fun drawRoundedRect(left: Int, top: Int, right: Int, bottom: Int, color: Int) =
        RoundedRectangleShader.performVQuadAndUniforms(
            SkyHanniRenderPipeline.ROUNDED_RECT(),
            x1 = left, y1 = top, x2 = right, y2 = bottom,
            postVertexOps = listOf { setColor(color) },
        )

    fun drawRoundedTexturedRect(left: Int, top: Int, right: Int, bottom: Int, texture: ResourceLocation) {
        //#if MC < 1.21.6
        val glTex = Minecraft.getInstance().textureManager.getTexture(texture).texture
        //#else
        //$$ val glTex = Minecraft.getInstance().textureManager.getTexture(texture).textureView
        //#endif
        RenderSystem.assertOnRenderThread()
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
            bindSampler("textureSampler", glTex)
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
            //#if MC > 1.21.6
            //$$ { roundedOutlineBufferSlice = roundedOutlineUniform.writeWith(
            //$$     RoundedRectangleOutlineShader.borderThickness, RoundedRectangleOutlineShader.borderBlur
            //$$ ) },
            //#endif
            withSmoothness = false,
        ) {
            //#if MC < 1.21.6
            setUniform("borderThickness", RoundedRectangleOutlineShader.borderThickness)
            setUniform("borderBlur", RoundedRectangleOutlineShader.borderBlur)
            //#else
            //$$ setUniform("SkyHanniRoundedOutlineUniforms", roundedOutlineBufferSlice)
            //#endif
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

    fun drawCircle(left: Int, top: Int, right: Int, bottom: Int, color: Int) =
        CircleShader.performVQuadAndUniforms(
            SkyHanniRenderPipeline.CIRCLE(),
            x1 = left, y1 = top, x2 = right, y2 = bottom,
            postVertexOps = listOf { setColor(color) },
            //#if MC > 1.21.6
            //$$ { circleBufferSlice = circleUniform.writeWith(
            //$$     CircleShader.angle1, CircleShader.angle2
            //$$ ) },
            //#endif
        ) {
            //#if MC < 1.21.6
            setUniform("angle1", CircleShader.angle1)
            setUniform("angle2", CircleShader.angle2)
            //#else
            //$$ setUniform("SkyHanniCircleUniforms", circleBufferSlice)
            //#endif
        }

    fun drawGradientCircle(left: Int, top: Int, right: Int, bottom: Int, startColor: ChromaColour, endColor: ChromaColour) =
        RadialGradientCircleShader.performVQuadAndUniforms(
            SkyHanniRenderPipeline.RADIAL_GRADIENT_CIRCLE(),
            x1 = left, y1 = top, x2 = right, y2 = bottom,
            postVertexOps = listOf(
                { setColor(startColor.toColor().rgb) },
                { setColor(endColor.toColor().rgb) },
            ),
            //#if MC > 1.21.6
            //$$ { radialGradientCircleBufferSlice = radialGradientCircleUniform.writeWith(
            //$$     RadialGradientCircleShader.angle,
            //$$     Vector4f(startColor.destructToFloatArray()),
            //$$     Vector4f(endColor.destructToFloatArray()),
            //$$     RadialGradientCircleShader.progress,
            //$$     RadialGradientCircleShader.phaseOffset,
            //$$     RadialGradientCircleShader.reverse
            //$$ ) },
            //#endif
        ) {
            //#if MC < 1.21.6
            val sc = startColor.destructToFloatArray()
            val ec = endColor.destructToFloatArray()
            setUniform("startColor", sc[0], sc[1], sc[2], sc[3])
            setUniform("endColor", ec[0], ec[1], ec[2], ec[3])
            setUniform("angle", RadialGradientCircleShader.angle)
            setUniform("progress", RadialGradientCircleShader.progress)
            setUniform("phaseOffset", RadialGradientCircleShader.phaseOffset)
            //#else
            //$$ setUniform("SkyHanniRadialGradientCircleUniforms", radialGradientCircleBufferSlice)
            //#endif
        }

    private fun ChromaColour.destructToFloatArray(): FloatArray {
        return floatArrayOf(
            this.toColor().red.toFloat() / 255f,
            this.toColor().green.toFloat() / 255f,
            this.toColor().blue.toFloat() / 255f,
            this.alpha.toFloat() / 255f,
        )
    }

    //#if MC > 1.21.6
    //$$ fun clearUniforms() {
    //$$     roundedUniform.clear()
    //$$     roundedOutlineUniform.clear()
    //$$     circleUniform.clear()
    //$$ }
    //#endif
}
