package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.GuiEditManager
import at.hannibal2.skyhanni.data.GuiEditManager.getAbsX
import at.hannibal2.skyhanni.data.GuiEditManager.getAbsY
import at.hannibal2.skyhanni.data.model.Graph
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderItemEvent
import at.hannibal2.skyhanni.events.RenderGuiItemOverlayEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.misc.PatcherFixes
//#if TODO
import at.hannibal2.skyhanni.features.misc.RoundedRectangleOutlineShader
import at.hannibal2.skyhanni.features.misc.RoundedRectangleShader
import at.hannibal2.skyhanni.features.misc.RoundedTextureShader
//#endif
import at.hannibal2.skyhanni.utils.ColorUtils.getFirstColorCode
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.LorenzColor.Companion.toLorenzColor
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils._draw3DLine
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils._draw3DPathWithWaypoint
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils._drawCircle
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils._drawColor
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils._drawCylinderInWorld
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils._drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils._drawFilledBoundingBox
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils._drawHitbox
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils._drawPyramid
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils._drawSphereInWorld
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils._drawSphereWireframeInWorld
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils._drawString
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils._drawWaypointFilled
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils._drawWireframeBoundingBox
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils._outlineTopFace
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXAligned
//#if TODO
import at.hannibal2.skyhanni.utils.shader.ShaderManager
//#endif
import io.github.notenoughupdates.moulconfig.ChromaColour
import net.minecraft.client.Minecraft
//#if TODO
import net.minecraft.client.renderer.GLAllocation
//#endif
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.inventory.Slot
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.nio.FloatBuffer
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.DurationUnit

@Suppress("LargeClass", "TooManyFunctions")
object RenderUtils {

    enum class HorizontalAlignment(private val value: String) {
        LEFT("Left"),
        CENTER("Center"),
        RIGHT("Right"),
        DONT_ALIGN("Don't Align"),
        ;

        override fun toString() = value
    }

    enum class VerticalAlignment(private val value: String) {
        TOP("Top"),
        CENTER("Center"),
        BOTTOM("Bottom"),
        DONT_ALIGN("Don't Align"),
        ;

        override fun toString() = value
    }

    //#if TODO
    private val matrixBuffer: FloatBuffer = GLAllocation.createDirectFloatBuffer(16)
    private val colorBuffer: FloatBuffer = GLAllocation.createDirectFloatBuffer(16)
    //#endif

    //#if MC < 1.8.9
    /**
     * Used for some debugging purposes.
     */
    val absoluteTranslation
        get() = run {
            matrixBuffer.clear()

            GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, matrixBuffer)

            val read = generateSequence(0) { it + 1 }.take(16).map { matrixBuffer.get() }.toList()

            val xTranslate = read[12].toInt()
            val yTranslate = read[13].toInt()
            val zTranslate = read[14].toInt()

            matrixBuffer.flip()

            Triple(xTranslate, yTranslate, zTranslate)
        }
    //#endif

    fun Slot.highlight(color: LorenzColor) {
        highlight(color.toColor())
    }

    // TODO eventually removed awt.Color support, we should only use moulconfig.ChromaColour or LorenzColor
    fun Slot.highlight(color: Color) {
        highlight(color, xDisplayPosition, yDisplayPosition)
    }

    fun Slot.highlight(color: ChromaColour) {
        highlight(color.toColor())
    }

    fun RenderGuiItemOverlayEvent.highlight(color: LorenzColor) {
        highlight(color.toColor())
    }

    fun RenderGuiItemOverlayEvent.highlight(color: Color) {
        highlight(color, x, y)
    }

    fun highlight(color: Color, x: Int, y: Int) {
        GlStateManager.disableLighting()
        GlStateManager.disableDepth()
        DrawContextUtils.pushMatrix()
        // TODO don't use z
        //#if TODO
        val zLevel = Minecraft.getMinecraft().renderItem.zLevel
        //#else
        //$$ val zLevel = 50f
        //#endif
        DrawContextUtils.translate(0f, 0f, 110 + zLevel)
        GuiRenderUtils.drawRect(x, y, x + 16, y + 16, color.rgb)
        DrawContextUtils.popMatrix()
        GlStateManager.enableDepth()
        GlStateManager.enableLighting()
    }

    fun Slot.drawBorder(color: LorenzColor) {
        drawBorder(color.toColor())
    }

    fun Slot.drawBorder(color: Color) {
        drawBorder(color, xDisplayPosition, yDisplayPosition)
    }

    fun RenderGuiItemOverlayEvent.drawBorder(color: LorenzColor) {
        drawBorder(color.toColor())
    }

    fun RenderGuiItemOverlayEvent.drawBorder(color: Color) {
        drawBorder(color, x, y)
    }

    fun drawBorder(color: Color, x: Int, y: Int) {
        GlStateManager.disableLighting()
        GlStateManager.disableDepth()
        DrawContextUtils.pushMatrix()
        //#if TODO
        val zLevel = Minecraft.getMinecraft().renderItem.zLevel
        //#else
        //$$ val zLevel = 50f
        //#endif
        DrawContextUtils.translate(0f, 0f, 110 + zLevel)
        GuiRenderUtils.drawRect(x, y, x + 1, y + 16, color.rgb)
        GuiRenderUtils.drawRect(x, y, x + 16, y + 1, color.rgb)
        GuiRenderUtils.drawRect(x, y + 15, x + 16, y + 16, color.rgb)
        GuiRenderUtils.drawRect(x + 15, y, x + 16, y + 16, color.rgb)
        DrawContextUtils.popMatrix()
        GlStateManager.enableDepth()
        GlStateManager.enableLighting()
    }

    @Deprecated("Use WorldRenderUtils' drawColor instead")
    fun SkyHanniRenderWorldEvent.drawColor(
        location: LorenzVec,
        color: LorenzColor,
        beacon: Boolean = false,
        alpha: Float = -1f,
        seeThroughBlocks: Boolean = true,
    ) {
        _drawColor(location, color.toColor(), beacon, alpha, seeThroughBlocks)
    }

    @Deprecated("Use WorldRenderUtils' drawColor instead")
    fun SkyHanniRenderWorldEvent.drawColor(
        location: LorenzVec,
        color: Color,
        beacon: Boolean = false,
        alpha: Float = -1f,
        seeThroughBlocks: Boolean = true,
    ) {
        _drawColor(location, color, beacon, alpha, seeThroughBlocks)
    }

    //#if TODO
    @Deprecated("Use WorldRenderUtils' getViewerPos instead", ReplaceWith("WorldRenderUtils.getViewerPos(partialTicks)"))
    fun getViewerPos(partialTicks: Float) =
        Minecraft.getMinecraft().renderViewEntity?.let { exactLocation(it, partialTicks) } ?: LorenzVec()
    //#endif

    @Deprecated("Use WorldRenderUtils' expandBlock instead")
    fun AxisAlignedBB.expandBlock(n: Int = 1) = expand(LorenzVec.expandVector * n)

    @Deprecated("Use WorldRenderUtils' inflateBlock instead")
    fun AxisAlignedBB.inflateBlock(n: Int = 1) = expand(LorenzVec.expandVector * -n)

    @Deprecated("Use WorldRenderUtils' drawWaypointFilled instead")
    fun SkyHanniRenderWorldEvent.drawWaypointFilled(
        location: LorenzVec,
        color: Color,
        seeThroughBlocks: Boolean = false,
        beacon: Boolean = false,
        extraSize: Double = 0.0,
        extraSizeTopY: Double = extraSize,
        extraSizeBottomY: Double = extraSize,
        minimumAlpha: Float = 0.2f,
        inverseAlphaScale: Boolean = false,
    ) {
        _drawWaypointFilled(
            location,
            color,
            seeThroughBlocks,
            beacon,
            extraSize,
            extraSizeTopY,
            extraSizeBottomY,
            minimumAlpha,
            inverseAlphaScale,
        )
    }

    @Deprecated("Use WorldRenderUtils' drawString instead")
    fun SkyHanniRenderWorldEvent.drawString(
        location: LorenzVec,
        text: String,
        seeThroughBlocks: Boolean = false,
        color: Color? = null,
    ) {
        _drawString(location, text, seeThroughBlocks, color)
    }

    fun interpolate(currentValue: Double, lastValue: Double, multiplier: Double): Double {
        return lastValue + (currentValue - lastValue) * multiplier
    }

    fun Position.transform(): Pair<Int, Int> {
        DrawContextUtils.translate(getAbsX().toFloat(), getAbsY().toFloat(), 0F)
        DrawContextUtils.scale(effectiveScale, effectiveScale, 1F)
        val x = ((GuiScreenUtils.mouseX - getAbsX()) / effectiveScale).toInt()
        val y = ((GuiScreenUtils.mouseY - getAbsY()) / effectiveScale).toInt()
        return x to y
    }

    fun Position.renderString(string: String?, offsetX: Int = 0, offsetY: Int = 0, posLabel: String) {
        if (string.isNullOrBlank()) return
        val x = renderString0(string, offsetX, offsetY, centerX)
        GuiEditManager.add(this, posLabel, x, 10)
    }

    private fun Position.renderString0(string: String, offsetX: Int = 0, offsetY: Int = 0, centered: Boolean): Int {
        val display = "§f$string"
        DrawContextUtils.pushMatrix()
        transform()
        val fr = Minecraft.getMinecraft().fontRendererObj

        DrawContextUtils.translate(offsetX + 1.0, offsetY + 1.0, 0.0)

        if (centered) {
            val strLen: Int = fr.getStringWidth(string)
            val x2 = offsetX - strLen / 2f
            GuiRenderUtils.drawString(display, x2, 0f, 0)
        } else {
            GuiRenderUtils.drawString(display, 0f, 0f, 0)
        }

        DrawContextUtils.popMatrix()

        return fr.getStringWidth(display)
    }

    fun Position.renderStrings(list: List<String>, extraSpace: Int = 0, posLabel: String) {
        if (list.isEmpty()) return

        var offsetY = 0
        var longestX = 0
        for (s in list) {
            val x = renderString0(s, offsetY = offsetY, centered = false)
            if (x > longestX) {
                longestX = x
            }
            offsetY += 10 + extraSpace
        }
        GuiEditManager.add(this, posLabel, longestX, offsetY)
    }

    fun Position.renderRenderables(
        renderables: List<Renderable>,
        extraSpace: Int = 0,
        posLabel: String,
        addToGuiManager: Boolean = true,
    ) {
        if (renderables.isEmpty()) return
        var longestY = 0
        val longestX = renderables.maxOf { it.width }
        for (line in renderables) {
            DrawContextUtils.pushMatrix()
            val (x, y) = transform()
            DrawContextUtils.translate(0f, longestY.toFloat(), 0F)
            Renderable.withMousePosition(x, y) {
                line.renderXAligned(0, longestY, longestX)
            }

            longestY += line.height + extraSpace + 2

            DrawContextUtils.popMatrix()
        }
        if (addToGuiManager) GuiEditManager.add(this, posLabel, longestX, longestY)
    }

    fun Position.renderRenderable(
        renderable: Renderable?,
        posLabel: String,
        addToGuiManager: Boolean = true,
    ) {
        // cause crashes and errors on purpose
        DrawContextUtils.drawContext
        if (renderable == null) return
        DrawContextUtils.pushMatrix()
        val (x, y) = transform()
        Renderable.withMousePosition(x, y) {
            renderable.render(0, 0)
        }
        DrawContextUtils.popMatrix()
        if (addToGuiManager) GuiEditManager.add(this, posLabel, renderable.width, renderable.height)
    }

    @Deprecated("Use WorldRenderUtils' drawCircle instead")
    fun SkyHanniRenderWorldEvent.drawCircle(entity: Entity, rad: Double, color: Color) {
        _drawCircle(entity, rad, color)
    }

    @Deprecated("Use WorldRenderUtils' drawCylinderInWorld instead")
    fun SkyHanniRenderWorldEvent.drawCylinderInWorld(
        color: Color,
        location: LorenzVec,
        radius: Float,
        height: Float,
    ) {
        _drawCylinderInWorld(color, location.x, location.y, location.z, radius, height)
    }

    @Deprecated("Use WorldRenderUtils' drawPyramid instead")
    fun SkyHanniRenderWorldEvent.drawPyramid(
        topPoint: LorenzVec,
        baseCenterPoint: LorenzVec,
        baseEdgePoint: LorenzVec,
        color: Color,
        depth: Boolean = true,
    ) {
        _drawPyramid(topPoint, baseCenterPoint, baseEdgePoint, color, depth)
    }

    @Deprecated("Use WorldRenderUtils' drawCylinderInWorld instead")
    fun SkyHanniRenderWorldEvent.drawCylinderInWorld(
        color: Color,
        x: Double,
        y: Double,
        z: Double,
        radius: Float,
        height: Float,
    ) {
        _drawCylinderInWorld(color, x, y, z, radius, height)
    }

    @Deprecated("Use WorldRenderUtils' drawSphereInWorld instead")
    fun SkyHanniRenderWorldEvent.drawSphereInWorld(
        color: Color,
        location: LorenzVec,
        radius: Float,
    ) {
        _drawSphereInWorld(color, location.x, location.y, location.z, radius)
    }

    @Deprecated("Use WorldRenderUtils' drawSphereInWorld instead")
    fun SkyHanniRenderWorldEvent.drawSphereInWorld(
        color: Color,
        x: Double,
        y: Double,
        z: Double,
        radius: Float,
    ) {
        _drawSphereInWorld(color, x, y, z, radius)
    }

    @Deprecated("Use WorldRenderUtils' drawSphereWireframeInWorld instead")
    fun SkyHanniRenderWorldEvent.drawSphereWireframeInWorld(
        color: Color,
        location: LorenzVec,
        radius: Float,
    ) {
        _drawSphereWireframeInWorld(color, location.x, location.y, location.z, radius)
    }

    @Deprecated("Use WorldRenderUtils' drawSphereWireframeInWorld instead")
    fun SkyHanniRenderWorldEvent.drawSphereWireframeInWorld(
        color: Color,
        x: Double,
        y: Double,
        z: Double,
        radius: Float,
    ) {
        _drawSphereWireframeInWorld(color, x, y, z, radius)
    }

    @Deprecated("Use WorldRenderUtils' drawDynamicText instead")
    fun SkyHanniRenderWorldEvent.drawDynamicText(
        location: LorenzVec,
        text: String,
        scaleMultiplier: Double,
        yOff: Float = 0f,
        hideTooCloseAt: Double = 4.5,
        smallestDistanceVew: Double = 5.0,
        ignoreBlocks: Boolean = true,
        ignoreY: Boolean = false,
        maxDistance: Int? = null,
    ) {
        _drawDynamicText(location, text, scaleMultiplier, yOff, hideTooCloseAt, smallestDistanceVew, ignoreBlocks, ignoreY, maxDistance)
    }

    @Deprecated("Use WorldRenderUtils' exactLocation instead")
    fun SkyHanniRenderWorldEvent.exactLocation(entity: Entity) = exactLocation(entity, partialTicks)

    @Deprecated("Use WorldRenderUtils' exactLocation instead")
    fun SkyHanniRenderWorldEvent.exactPlayerEyeLocation(): LorenzVec {
        val player = MinecraftCompat.localPlayer
        val eyeHeight = player.getEyeHeight().toDouble()
        //#if TODO
        PatcherFixes.onPlayerEyeLine()
        //#endif
        return exactLocation(player).add(y = eyeHeight)
    }

    @Deprecated("Use WorldRenderUtils' exactBoundingBox instead")
    fun SkyHanniRenderWorldEvent.exactBoundingBox(entity: Entity): AxisAlignedBB {
        if (entity.isDead) return entity.entityBoundingBox
        val offset = exactLocation(entity) - entity.getLorenzVec()
        return entity.entityBoundingBox.offset(offset.x, offset.y, offset.z)
    }

    @Deprecated("Use WorldRenderUtils' exactPlayerEyeLocation instead")
    fun SkyHanniRenderWorldEvent.exactPlayerEyeLocation(player: Entity): LorenzVec {
        val add = if (player.isSneaking) LorenzVec(0.0, 1.54, 0.0) else LorenzVec(0.0, 1.62, 0.0)
        return exactLocation(player) + add
    }

    fun SkyHanniRenderWorldEvent.drawLineToEye(location: LorenzVec, color: Color, lineWidth: Int, depth: Boolean) {
        _draw3DLine(exactPlayerEyeLocation(), location, color, lineWidth, depth)
    }

    @Deprecated("Use WorldRenderUtils' exactLocation instead")
    fun exactLocation(entity: Entity, partialTicks: Float): LorenzVec {
        return WorldRenderUtils.exactLocation(entity, partialTicks)
    }

    @Deprecated("Use WorldRenderUtils' drawWireframeBoundingBox instead")
    fun SkyHanniRenderWorldEvent.drawWireframeBoundingBox(
        aabb: AxisAlignedBB,
        color: Color,
    ) {
        _drawWireframeBoundingBox(aabb, color)
    }

    @Deprecated("Use WorldRenderUtils' draw3DPathWithWaypoint instead")
    fun SkyHanniRenderWorldEvent.draw3DPathWithWaypoint(
        path: Graph,
        colorLine: Color,
        lineWidth: Int,
        depth: Boolean,
        startAtEye: Boolean = true,
        textSize: Double = 1.0,
        waypointColor: Color =
            (path.lastOrNull()?.name?.getFirstColorCode()?.toLorenzColor() ?: LorenzColor.WHITE).toColor(),
        bezierPoint: Double = 1.0,
        showNodeNames: Boolean = false,
        markLastBlock: Boolean = true,
    ) {
        _draw3DPathWithWaypoint(
            path,
            colorLine,
            lineWidth,
            depth,
            startAtEye,
            textSize,
            waypointColor,
            bezierPoint,
            showNodeNames,
            markLastBlock,
        )
    }

    @Deprecated("Use WorldRenderUtils' drawFilledBoundingBox instead")
    fun SkyHanniRenderWorldEvent.drawFilledBoundingBox(
        aabb: AxisAlignedBB,
        c: Color,
        alphaMultiplier: Float = 1f,
        /**
         * If set to `true`, renders the box relative to the camera instead of relative to the world.
         * If set to `false`, will be relativized to [RenderUtils.getViewerPos].
         */
        renderRelativeToCamera: Boolean = false,
        drawVerticalBarriers: Boolean = true,
    ) {
        _drawFilledBoundingBox(aabb, c, alphaMultiplier, renderRelativeToCamera, drawVerticalBarriers)
    }

    @Deprecated("Use WorldRenderUtils' outlineTopFace instead")
    fun SkyHanniRenderWorldEvent.outlineTopFace(
        boundingBox: AxisAlignedBB,
        lineWidth: Int,
        color: Color,
        depth: Boolean,
    ) {
        _outlineTopFace(boundingBox, lineWidth, color, depth)
    }

    @Deprecated("Use WorldRenderUtils' draw3DLine instead")
    fun SkyHanniRenderWorldEvent.draw3DLine(
        p1: LorenzVec,
        p2: LorenzVec,
        color: Color,
        lineWidth: Int,
        depth: Boolean,
    ) {
        _draw3DLine(p1, p2, color, lineWidth, depth)
    }

    @Deprecated("Use WorldRenderUtils' drawHitbox instead")
    fun SkyHanniRenderWorldEvent.drawHitbox(
        boundingBox: AxisAlignedBB,
        lineWidth: Int,
        color: Color,
        depth: Boolean,
    ) {
        _drawHitbox(boundingBox, lineWidth, color, depth)
    }

    fun chromaColor(
        timeTillRepeat: Duration,
        offset: Float = 0f,
        saturation: Float = 1F,
        brightness: Float = 0.8F,
        timeOverride: Long = System.currentTimeMillis(),
    ): Color {
        return Color(
            Color.HSBtoRGB(
                ((offset + timeOverride / timeTillRepeat.toDouble(DurationUnit.MILLISECONDS)) % 1).toFloat(),
                saturation,
                brightness,
            ),
        )
    }

    fun GuiRenderItemEvent.RenderOverlayEvent.GuiRenderItemPost.drawSlotText(
        xPos: Int,
        yPos: Int,
        text: String,
        scale: Float,
    ) {
        drawSlotText0(xPos, yPos, text, scale)
    }

    fun GuiContainerEvent.ForegroundDrawnEvent.drawSlotText(
        xPos: Int,
        yPos: Int,
        text: String,
        scale: Float,
    ) {
        drawSlotText0(xPos, yPos, text, scale)
    }

    private fun drawSlotText0(
        xPos: Int,
        yPos: Int,
        text: String,
        scale: Float,
    ) {
        val fontRenderer = Minecraft.getMinecraft().fontRendererObj

        GlStateManager.disableLighting()
        GlStateManager.disableDepth()
        GlStateManager.disableBlend()

        DrawContextUtils.pushMatrix()
        DrawContextUtils.translate((xPos - fontRenderer.getStringWidth(text)).toFloat(), yPos.toFloat(), 0f)
        DrawContextUtils.scale(scale, scale, 1f)
        GuiRenderUtils.drawString(text, 0f, 0f, 16777215)

        val reverseScale = 1 / scale

        DrawContextUtils.scale(reverseScale, reverseScale, 1f)
        DrawContextUtils.popMatrix()

        GlStateManager.enableLighting()
        GlStateManager.enableDepth()
    }

    /**
     * Method to draw a rounded textured rect.
     *
     * **NOTE:** If you are using [GlStateManager.translate] or [GlStateManager.scale]
     * with this method, ensure they are invoked in the correct order if you use both. That is, [GlStateManager.translate]
     * is called **BEFORE** [GlStateManager.scale], otherwise the textured rect will not be rendered correctly
     *
     * @param filter the texture filter to use
     * @param radius the radius of the corners (default 10), NOTE: If you pass less than 1 it will just draw as a normal textured rect
     * @param smoothness how smooth the corners will appear (default 1). NOTE: This does very
     * little to the smoothness of the corners in reality due to how the final pixel color is calculated.
     * It is best kept at its default.
     */
    fun drawRoundTexturedRect(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        filter: Int,
        radius: Int = 10,
        smoothness: Int = 1,
        texture: ResourceLocation,
        alpha: Float = 1f,
    ) {
        // if radius is 0 then just draw a normal textured rect
        if (radius <= 0) {
            GuiRenderUtils.drawTexturedRect(x, y, width, height, filter = filter, texture = texture, alpha = alpha)
            return
        }

        val scaleFactor = GuiScreenUtils.scaleFactor
        val widthIn = width * scaleFactor
        val heightIn = height * scaleFactor
        val xIn = x * scaleFactor
        val yIn = y * scaleFactor

        //#if TODO
        RoundedTextureShader.scaleFactor = scaleFactor.toFloat()
        RoundedTextureShader.radius = radius.toFloat()
        RoundedTextureShader.smoothness = smoothness.toFloat()
        RoundedTextureShader.halfSize = floatArrayOf(widthIn / 2f, heightIn / 2f)
        RoundedTextureShader.centerPos = floatArrayOf(xIn + (widthIn / 2f), yIn + (heightIn / 2f))
        //#endif

        DrawContextUtils.pushMatrix()
        //#if TODO
        ShaderManager.enableShader(ShaderManager.Shaders.ROUNDED_TEXTURE)
        //#endif

        GuiRenderUtils.drawTexturedRect(x, y, width, height, filter = filter, texture = texture, alpha = alpha)

        //#if TODO
        ShaderManager.disableShader()
        //#endif
        DrawContextUtils.popMatrix()
    }

    /**
     * Method to draw a rounded rectangle.
     *
     * **NOTE:** If you are using [GlStateManager.translate] or [GlStateManager.scale]
     * with this method, ensure they are invoked in the correct order if you use both. That is, [GlStateManager.translate]
     * is called **BEFORE** [GlStateManager.scale], otherwise the rectangle will not be rendered correctly
     *
     * @param color color of rect
     * @param radius the radius of the corners (default 10)
     * @param smoothness how smooth the corners will appear (default 1). NOTE: This does very
     * little to the smoothness of the corners in reality due to how the final pixel color is calculated.
     * It is best kept at its default.
     */
    fun drawRoundRect(x: Int, y: Int, width: Int, height: Int, color: Int, radius: Int = 10, smoothness: Int = 1) {
        val scaleFactor = GuiScreenUtils.scaleFactor
        val widthIn = width * scaleFactor
        val heightIn = height * scaleFactor
        val xIn = x * scaleFactor
        val yIn = y * scaleFactor

        //#if TODO
        RoundedRectangleShader.scaleFactor = scaleFactor.toFloat()
        RoundedRectangleShader.radius = radius.toFloat()
        RoundedRectangleShader.smoothness = smoothness.toFloat()
        RoundedRectangleShader.halfSize = floatArrayOf(widthIn / 2f, heightIn / 2f)
        RoundedRectangleShader.centerPos = floatArrayOf(xIn + (widthIn / 2f), yIn + (heightIn / 2f))
        //#endif

        DrawContextUtils.pushMatrix()
        //#if TODO
        ShaderManager.enableShader(ShaderManager.Shaders.ROUNDED_RECTANGLE)
        //#endif

        GuiRenderUtils.drawRect(x - 5, y - 5, x + width + 5, y + height + 5, color)

        //#if TODO
        ShaderManager.disableShader()
        //#endif
        DrawContextUtils.popMatrix()
    }

    /**
     * Method to draw a rounded rectangle.
     *
     * **NOTE:** If you are using [GlStateManager.translate] or [GlStateManager.scale]
     * with this method, ensure they are invoked in the correct order if you use both. That is, [GlStateManager.translate]
     * is called **BEFORE** [GlStateManager.scale], otherwise the rectangle will not be rendered correctly
     *
     * @param color color of rect
     * @param radius the radius of the corners (default 10)
     * @param smoothness how smooth the corners will appear (default 1). NOTE: This does very
     * little to the smoothness of the corners in reality due to how the final pixel color is calculated.
     * It is best kept at its default.
     */
    fun drawRoundGradientRect(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        topColor: Int,
        bottomColor: Int,
        radius: Int = 10,
        smoothness: Int = 1,
    ) {
        val scaledRes = GuiScreenUtils.scaleFactor
        val widthIn = width * scaledRes
        val heightIn = height * scaledRes
        val xIn = x * scaledRes
        val yIn = y * scaledRes

        //#if TODO
        RoundedRectangleShader.scaleFactor = scaledRes.toFloat()
        RoundedRectangleShader.radius = radius.toFloat()
        RoundedRectangleShader.smoothness = smoothness.toFloat()
        RoundedRectangleShader.halfSize = floatArrayOf(widthIn / 2f, heightIn / 2f)
        RoundedRectangleShader.centerPos = floatArrayOf(xIn + (widthIn / 2f), yIn + (heightIn / 2f))

        DrawContextUtils.pushMatrix()
        ShaderManager.enableShader(ShaderManager.Shaders.ROUNDED_RECTANGLE)

        GuiRenderUtils.drawGradientRect(x - 5, y - 5, x + width + 5, y + height + 5, topColor, bottomColor)

        ShaderManager.disableShader()
        DrawContextUtils.popMatrix()
        //#endif
    }

    /**
     * Method to draw the outline of a rounded rectangle with a color gradient. For a single color just pass
     * in the color to both topColor and bottomColor.
     *
     * This is *not* a method that draws a rounded rectangle **with** an outline, rather, this draws **only** the outline.
     *
     * **NOTE:** The same notices given from [drawRoundRect] should be acknowledged with this method also.
     *
     * @param topColor color of the top of the outline
     * @param bottomColor color of the bottom of the outline
     * @param borderThickness the thickness of the border
     * @param radius radius of the corners of the rectangle (default 10)
     * @param blur the amount to blur the outline (default 0.7f)
     */
    fun drawRoundRectOutline(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        topColor: Int,
        bottomColor: Int,
        borderThickness: Int,
        radius: Int = 10,
        blur: Float = 0.7f,
    ) {
        val scaleFactor = GuiScreenUtils.scaleFactor
        val widthIn = width * scaleFactor
        val heightIn = height * scaleFactor
        val xIn = x * scaleFactor
        val yIn = y * scaleFactor

        val borderAdjustment = borderThickness / 2

        //#if TODO
        RoundedRectangleOutlineShader.scaleFactor = scaleFactor.toFloat()
        RoundedRectangleOutlineShader.radius = radius.toFloat()
        RoundedRectangleOutlineShader.halfSize = floatArrayOf(widthIn / 2f, heightIn / 2f)
        RoundedRectangleOutlineShader.centerPos = floatArrayOf(xIn + (widthIn / 2f), yIn + (heightIn / 2f))
        RoundedRectangleOutlineShader.borderThickness = borderThickness.toFloat()
        // The blur argument is a bit misleading, the greater the value the more sharp the edges of the
        // outline will be and the smaller the value the blurrier. So we take the difference from 1
        // so the shader can blur the edges accordingly. This is because a 'blurriness' option makes more sense
        // to users than a 'sharpness' option in this context
        RoundedRectangleOutlineShader.borderBlur = max(1 - blur, 0f)
        //#endif

        DrawContextUtils.pushMatrix()
        //#if TODO
        ShaderManager.enableShader(ShaderManager.Shaders.ROUNDED_RECT_OUTLINE)
        //#endif

        GuiRenderUtils.drawGradientRect(
            x - borderAdjustment,
            y - borderAdjustment,
            x + width + borderAdjustment,
            y + height + borderAdjustment,
            topColor,
            bottomColor,
        )

        //#if TODO
        ShaderManager.disableShader()
        //#endif
        DrawContextUtils.popMatrix()
    }

    //#if TODO
    fun getAlpha(): Float {
        colorBuffer.clear()
        GlStateManager.getFloat(GL11.GL_CURRENT_COLOR, colorBuffer)
        if (colorBuffer.limit() < 4) return 1f
        return colorBuffer.get(3)
    }
    //#endif
}
