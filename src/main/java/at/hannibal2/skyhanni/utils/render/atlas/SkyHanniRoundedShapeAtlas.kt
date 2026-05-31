package at.hannibal2.skyhanni.utils.render.atlas

import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.state.gui.BlitRenderState
import net.minecraft.client.renderer.state.gui.GuiRenderState
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import org.joml.Matrix3x2f
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode

//? if < 26.1 {
/*import at.hannibal2.skyhanni.shader.CircleShader
import at.hannibal2.skyhanni.shader.RoundedRectangleShader
import at.hannibal2.skyhanni.shader.RoundedShader
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import at.hannibal2.skyhanni.utils.render.RoundedShapeDrawer
import at.hannibal2.skyhanni.utils.render.SkyHanniRenderPipeline
import com.mojang.blaze3d.ProjectionType
import com.mojang.blaze3d.vertex.Tesselator
import net.minecraft.client.renderer.ProjectionMatrixBuffer
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import java.util.OptionalDouble
import java.util.OptionalInt
*///?}

internal class SkyHanniRoundedShapeAtlas : SkyHanniAbstractAtlas<SkyHanniRoundedShapeAtlasKey, SkyHanniRoundedShapeAtlasEntry>() {

    override val identifier: Identifier by lazy {
        Identifier.fromNamespaceAndPath("skyhanni", "rounded_shape_atlas")
    }

    companion object {
        private const val PADDING = 5
    }

    override val colorLabel = "SkyHanni rounded shape atlas"
    override val depthLabel = "SkyHanni rounded shape atlas depth"

    //? if < 26.1 {
    /*private var projectionBuffer: ProjectionMatrixBuffer? = null

    private fun getOrCreateProjectionBuffer(): ProjectionMatrixBuffer =
        projectionBuffer ?: ProjectionMatrixBuffer(
            "SkyHanni rounded shape atlas", 1000.0f, 11000.0f, true,
        ).also { projectionBuffer = it }
    *///?}

    /**
     * Pre-renders any new static [shapes] into atlas slots before the GUI render pass.
     * No-op on 26.1+, where all shapes use the deferred rendering path.
     */
    fun preRenderShapes(shapes: List<SkyHanniRoundedShapeAtlasKey>) {
        //? if < 26.1 {
        /*if (shapes.isEmpty()) return
        ensureAllocated()

        val toRender = shapes.filter { it !in entries }
        if (toRender.isEmpty()) return

        val sizeF = sizePixels.toFloat()
        val projBuf = getOrCreateProjectionBuffer().getBuffer(sizeF, sizeF)
        RenderSystem.backupProjectionMatrix()
        RenderSystem.setProjectionMatrix(projBuf, ProjectionType.ORTHOGRAPHIC)

        for (key in toRender) {
            val pixelSize = maxOf(key.pixelWidth, key.pixelHeight)
            val slotSize = pixelSize + PADDING * 2
            val node = tryInsert(slotSize) ?: continue
            val slotX = node.x + PADDING
            val slotY = node.y + PADDING
            renderKeyToSlot(key, slotX, slotY, pixelSize)
            val (u, v) = uvForSlot(node.x, node.y)
            entries[key] = SkyHanniRoundedShapeAtlasEntry(node.x, node.y, u, v, slotSize)
        }

        RenderSystem.restoreProjectionMatrix()
        *///?}
    }

    //? if < 26.1 {
    /*/**
     * Configures [shader] uniforms for rendering [key] centered at the atlas slot ([slotX], [slotY]).
     *
     * The [SkyHanniRoundedShapeAtlasKey.smoothness] is translated into atlas pixel space.
     * The center Y is pre-adjusted for the [RoundedShader.centerPos] setter's
     * [GuiScreenUtils.displayHeight] flip so that the shader receives correct bottom-up coordinates.
     *
     * @param shader the rounded shader singleton to configure
     * @param key the shape key describing this shape's visual parameters
     * @param slotX left edge of the shape in atlas pixels (top-down, padding already stripped)
     * @param slotY top edge of the shape in atlas pixels (top-down, padding already stripped)
     */
    private fun setupAtlasUniforms(
        shader: RoundedShader<*>,
        key: SkyHanniRoundedShapeAtlasKey,
        slotX: Int,
        slotY: Int,
    ) {
        val centerX = slotX + key.pixelWidth / 2f
        // gl_FragCoord.y is bottom-up; the shader center must match.
        // The centerPos setter does:  field.y = displayHeight - value.y
        // We want field.y = atlasSize - (slotY + pixelHeight/2), so:
        //   value.y = displayHeight - atlasSize + slotY + pixelHeight/2
        val centerYInput = GuiScreenUtils.displayHeight - sizePixels + slotY + key.pixelHeight / 2f

        shader.scaleFactor = 1.0f
        shader.radius = when (key) {
            is SkyHanniRoundedShapeAtlasKey.RoundedRect -> key.radius.toFloat()
            is SkyHanniRoundedShapeAtlasKey.Circle -> key.radiusPixels.toFloat()
        }
        shader.smoothness = key.smoothness
        shader.halfSize = floatArrayOf(key.pixelWidth / 2f, key.pixelHeight / 2f)
        shader.centerPos = floatArrayOf(centerX, centerYInput)
        shader.modelViewMatrix = Matrix4f()
    }

    /**
     * Renders a single [key] into the atlas at ([slotX], [slotY]) using the immediate-mode pipeline.
     * Assumes the projection matrix is already set to atlas-space orthographic.
     *
     * @param key the shape to render
     * @param slotX left edge of the render area in atlas pixels (top-down, padding already stripped)
     * @param slotY top edge of the render area in atlas pixels (top-down, padding already stripped)
     * @param pixelSize bounding pixel dimension of the shape
     */
    private fun renderKeyToSlot(
        key: SkyHanniRoundedShapeAtlasKey,
        slotX: Int,
        slotY: Int,
        pixelSize: Int,
    ) {
        val left = (slotX - PADDING).toFloat()
        val top = (slotY - PADDING).toFloat()
        val right = (slotX + pixelSize + PADDING).toFloat()
        val bottom = (slotY + pixelSize + PADDING).toFloat()

        val shader: RoundedShader<*>
        val pipeline: com.mojang.blaze3d.pipeline.RenderPipeline
        val color: Int = when (key) {
            is SkyHanniRoundedShapeAtlasKey.RoundedRect -> {
                shader = RoundedRectangleShader
                pipeline = SkyHanniRenderPipeline.ROUNDED_RECT()
                key.color
            }
            is SkyHanniRoundedShapeAtlasKey.Circle -> {
                CircleShader.angle1 = key.angle1 - Math.PI.toFloat()
                CircleShader.angle2 = key.angle2 - Math.PI.toFloat()
                shader = CircleShader
                pipeline = SkyHanniRenderPipeline.CIRCLE()
                key.color
            }
        }

        setupAtlasUniforms(shader, key, slotX, slotY)

        val buffer = Tesselator.getInstance().begin(pipeline.vertexFormatMode, pipeline.vertexFormat)
        listOf(left to top, left to bottom, right to bottom, right to top).forEach { (x, y) ->
            buffer.addVertex(x, y, 0f).setColor(color)
        }
        val mesh = buffer.buildOrThrow()
        val vertexBuffer = pipeline.vertexFormat.uploadImmediateVertexBuffer(mesh.vertexBuffer())
        val seqBuffer = RenderSystem.getSequentialBuffer(mesh.drawState().mode)
        val indexBuffer = seqBuffer.getBuffer(mesh.drawState().indexCount)
        val indexType = seqBuffer.type()

        val dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(
            Matrix4f().setTranslation(0.0f, 0.0f, -11000.0f),
            Vector4f(1.0f, 1.0f, 1.0f, 1.0f),
            Vector3f(),
            Matrix4f(),
        )

        RoundedShapeDrawer.roundedBufferSlice = RoundedShapeDrawer.roundedUniform.writeWith(
            shader.scaleFactor, shader.radius, shader.smoothness,
            shader.halfSize, shader.centerPos, shader.modelViewMatrix,
        )

        RenderSystem.enableScissorForRenderTypeDraws(
            slotX - PADDING, sizePixels - slotY - pixelSize - PADDING, pixelSize + PADDING * 2, pixelSize + PADDING * 2,
        )

        @Suppress("UnsafeCallOnNullableType")
        RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            { "SkyHanni Atlas Shape Render" },
            textureView!!,
            OptionalInt.empty(),
            depthTextureView!!,
            OptionalDouble.empty(),
        ).use { pass ->
            RenderSystem.bindDefaultUniforms(pass)
            pass.setUniform("DynamicTransforms", dynamicTransforms)
            pass.setUniform("SkyHanniRoundedUniforms", RoundedShapeDrawer.roundedBufferSlice)

            if (key is SkyHanniRoundedShapeAtlasKey.Circle) {
                RoundedShapeDrawer.circleBufferSlice = RoundedShapeDrawer.circleUniform.writeWith(
                    CircleShader.angle1, CircleShader.angle2,
                )
                pass.setUniform("SkyHanniCircleUniforms", RoundedShapeDrawer.circleBufferSlice)
            }

            pass.setPipeline(pipeline)
            pass.setVertexBuffer(0, vertexBuffer)
            pass.setIndexBuffer(indexBuffer, indexType)
            pass.drawIndexed(0, 0, mesh.drawState().indexCount, 1)
        }

        RenderSystem.disableScissorForRenderTypeDraws()
        mesh.close()
    }
    *///?}

    /**
     * Submits a blit for a shape already in the atlas. Returns false if the shape has not been
     * pre-rendered yet (atlas miss), in which case the caller should submit a deferred state instead.
     *
     * @param key the atlas key identifying the shape to blit
     * @param guiRenderState the current frame's GUI render state queue
     * @param pose the current GUI pose matrix, used for coordinate transformation
     * @param x0 left screen coordinate in logical GUI pixels
     * @param y0 top screen coordinate in logical GUI pixels
     * @param x1 right screen coordinate in logical GUI pixels
     * @param y1 bottom screen coordinate in logical GUI pixels
     * @param alpha ARGB-packed alpha multiplier; use -1 for fully opaque white
     * @param scissor optional scissor rectangle to clip the blit
     */
    fun submitBlit(
        key: SkyHanniRoundedShapeAtlasKey,
        guiRenderState: GuiRenderState,
        pose: Matrix3x2f,
        x0: Int, y0: Int, x1: Int, y1: Int,
        alpha: Int,
        scissor: ScreenRectangle?,
    ): Boolean {
        val entry = entries[key] ?: return false
        val size = sizePixels.toFloat()
        // Strip the slot padding from the UV so the content maps 1:1 to the screen rect.
        // entry.u/v is the top-left of the slot; content starts PADDING pixels inside.
        val pad = PADDING / size
        val uContent = entry.u + pad
        val vContent = entry.v - pad
        val u1 = uContent + key.pixelWidth / size
        val v1 = vContent - key.pixelHeight / size
        guiRenderState.addBlitToCurrentLayer(
            BlitRenderState(
                RenderPipelines.GUI_TEXTURED,
                @Suppress("UnsafeCallOnNullableType")
                TextureSetup.singleTexture(textureView!!, RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
                pose,
                x0, y0, x1, y1,
                uContent, u1, vContent, v1,
                alpha,
                scissor,
            )
        )
        return true
    }

    override fun close() {
        super.close()
        //? if < 26.1 {
        /*projectionBuffer?.close()
        projectionBuffer = null
        *///?}
    }
}
