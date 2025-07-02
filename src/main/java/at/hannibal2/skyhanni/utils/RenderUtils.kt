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
import at.hannibal2.skyhanni.utils.ColorUtils.getFirstColorCode
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.LorenzColor.Companion.toLorenzColor
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils._draw3DLine
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils._draw3DPathWithWaypoint
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils._drawColor
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils._drawCylinderInWorld
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils._drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils._drawFilledBoundingBox
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils._drawHitbox
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils._drawLineToEye
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils._drawPyramid
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils._drawSphereInWorld
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils._drawSphereWireframeInWorld
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils._drawString
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils._drawWaypointFilled
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils._outlineTopFace
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXAligned
import io.github.notenoughupdates.moulconfig.ChromaColour
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.inventory.Slot
import net.minecraft.util.AxisAlignedBB
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.nio.FloatBuffer
import kotlin.time.Duration
import kotlin.time.DurationUnit
//#if MC < 1.21
import net.minecraft.client.renderer.GLAllocation
//#else
//$$ import com.mojang.blaze3d.systems.RenderSystem
//$$ import org.lwjgl.BufferUtils
//#endif

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

    //#if MC < 1.21
    private val matrixBuffer: FloatBuffer = GLAllocation.createDirectFloatBuffer(16)
    private val colorBuffer: FloatBuffer = GLAllocation.createDirectFloatBuffer(16)
    //#endif

    /**
     * Used for some debugging purposes.
     */
    val absoluteTranslation
        get() = run {
            //#if MC < 1.21
            matrixBuffer.clear()
            GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, matrixBuffer)
            val read = generateSequence(0) { it + 1 }.take(16).map { matrixBuffer.get() }.toList()
            val xTranslate = read[12].toInt()
            val yTranslate = read[13].toInt()
            val zTranslate = read[14].toInt()
            matrixBuffer.flip()
            //#else
            //$$ RenderSystem.assertOnRenderThread()
            //$$ val posMatrix = DrawContextUtils.drawContext.matrices.peek().positionMatrix
            //$$ val tmp = org.joml.Vector3f()
            //$$ posMatrix.getTranslation(tmp)
            //$$ val xTranslate = tmp.x.toInt()
            //$$ val yTranslate = tmp.y.toInt()
            //$$ val zTranslate = tmp.z.toInt()
            //#endif
            Triple(xTranslate, yTranslate, zTranslate)
        }

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

    private fun highlight(color: Color, x: Int, y: Int) {
        GlStateManager.disableLighting()
        GlStateManager.disableDepth()
        DrawContextUtils.pushMatrix()
        // TODO don't use z
        //#if MC < 1.21
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
            GuiRenderUtils.drawString(display, x2, 0f, -1)
        } else {
            GuiRenderUtils.drawString(display, 0f, 0f, -1)
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
        segments: Int = 32,
    ) {
        _drawSphereInWorld(color, location.x, location.y, location.z, radius, segments)
    }

    @Deprecated("Use WorldRenderUtils' drawSphereInWorld instead")
    fun SkyHanniRenderWorldEvent.drawSphereInWorld(
        color: Color,
        x: Double,
        y: Double,
        z: Double,
        radius: Float,
        segments: Int = 32,
    ) {
        _drawSphereInWorld(color, x, y, z, radius, segments)
    }

    @Deprecated("Use WorldRenderUtils' drawSphereWireframeInWorld instead")
    fun SkyHanniRenderWorldEvent.drawSphereWireframeInWorld(
        color: Color,
        location: LorenzVec,
        radius: Float,
        segments: Int = 32,
    ) {
        _drawSphereWireframeInWorld(color, location.x, location.y, location.z, radius, segments)
    }

    @Deprecated("Use WorldRenderUtils' drawSphereWireframeInWorld instead")
    fun SkyHanniRenderWorldEvent.drawSphereWireframeInWorld(
        color: Color,
        x: Double,
        y: Double,
        z: Double,
        radius: Float,
        segments: Int = 32,
    ) {
        _drawSphereWireframeInWorld(color, x, y, z, radius, segments)
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
        // TODO cache once per frame
        val player = MinecraftCompat.localPlayer
        val eyeHeight = player.getEyeHeight().toDouble()
        PatcherFixes.onPlayerEyeLine()
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

    @Deprecated("Use WorldRenderUtils' drawLineToEye instead")
    fun SkyHanniRenderWorldEvent.drawLineToEye(location: LorenzVec, color: Color, lineWidth: Int, depth: Boolean) {
        _drawLineToEye(location, color, lineWidth, depth)
    }

    @Deprecated("Use WorldRenderUtils' exactLocation instead")
    fun exactLocation(entity: Entity, partialTicks: Float): LorenzVec {
        return WorldRenderUtils.exactLocation(entity, partialTicks)
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
        color: Color,
        lineWidth: Int = 3,
        depth: Boolean = true,
    ) {
        _drawHitbox(boundingBox, color, lineWidth, depth)
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

        DrawContextUtils.pushPop {
            DrawContextUtils.translate((xPos - fontRenderer.getStringWidth(text)).toFloat(), yPos.toFloat(), 200f)
            DrawContextUtils.scale(scale, scale, 1f)
            GuiRenderUtils.drawString(text, 0f, 0f, -1)

            val reverseScale = 1 / scale

            DrawContextUtils.scale(reverseScale, reverseScale, 1f)
        }

        GlStateManager.enableLighting()
        GlStateManager.enableDepth()
    }

    //#if MC < 1.21
    fun getAlpha(): Float {
        colorBuffer.clear()
        GlStateManager.getFloat(GL11.GL_CURRENT_COLOR, colorBuffer)
        if (colorBuffer.limit() < 4) return 1f
        return colorBuffer.get(3)
    }
    //#endif
}
