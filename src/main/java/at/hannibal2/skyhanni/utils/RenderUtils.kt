package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.GuiEditManager
import at.hannibal2.skyhanni.data.GuiEditManager.getAbsX
import at.hannibal2.skyhanni.data.GuiEditManager.getAbsY
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderItemEvent
import at.hannibal2.skyhanni.events.RenderGuiItemOverlayEvent
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXAligned
import io.github.notenoughupdates.moulconfig.ChromaColour
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.inventory.Slot
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
            //#elseif MC < 1.21.6
            //$$ RenderSystem.assertOnRenderThread()
            //$$ val posMatrix = DrawContextUtils.drawContext.matrices.peek().positionMatrix
            //$$ val tmp = org.joml.Vector3f()
            //$$ posMatrix.getTranslation(tmp)
            //$$ val xTranslate = tmp.x.toInt()
            //$$ val yTranslate = tmp.y.toInt()
            //$$ val zTranslate = tmp.z.toInt()
            //#else
            //$$ val xTranslate = 0
            //$$ val yTranslate = 0
            //$$ val zTranslate = 0
            //#endif
            Triple(xTranslate, yTranslate, zTranslate)
        }

    // todo move to GuiRenderUtils?
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

    @Deprecated("Use renderRenderable instead", ReplaceWith("renderRenderable(renderable, posLabel)"))
    fun Position.renderString(string: String?, offsetX: Int = 0, offsetY: Int = 0, posLabel: String) {
        if (string.isNullOrBlank()) return
        val x = renderString0(string, offsetX, offsetY, centerX)
        GuiEditManager.add(this, posLabel, x, 10)
    }

    @Deprecated("Use renderRenderable instead", ReplaceWith("renderRenderable(renderable, posLabel)"))
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

    @Deprecated("Use renderRenderables instead", ReplaceWith("renderRenderables(renderables)"))
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

    @Deprecated("Use ChromaColor instead")
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

    // todo move to GuiRenderUtils?
    fun GuiRenderItemEvent.RenderOverlayEvent.GuiRenderItemPost.drawSlotText(
        xPos: Int,
        yPos: Int,
        text: String,
        scale: Float,
    ) {
        drawSlotText0(xPos, yPos, text, scale)
    }

    // todo move to GuiRenderUtils?
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
