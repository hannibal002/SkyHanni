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
import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.systems.RenderSystem
import io.github.notenoughupdates.moulconfig.ChromaColour
import net.minecraft.client.Minecraft
import net.minecraft.world.inventory.Slot
import java.awt.Color
import java.util.concurrent.CompletableFuture

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

    /**
     * Runs a block on an asserted Render Thread.
     * @param block the block to run
     */
    private fun <T> runOnRenderThread(
        setupFor: Lighting.Entry? = null,
        block: () -> T,
    ): T {
        RenderSystem.assertOnRenderThread()
        setupFor?.let { Minecraft.getInstance().gameRenderer.lighting.setupFor(it) }
        return block()
    }

    /**
     * Returns a [Thread] that schedules a block on the Render Thread when started.
     * Useful for [Runtime.addShutdownHook].
     */
    fun threadOnRenderThread(
        setupFor: Lighting.Entry? = null,
        block: () -> Any,
    ) = Thread { scheduleOnRenderThread(setupFor, block) }

    /**
     * Runs or schedules a block on the Render Thread.
     * - If already on the render thread, executes immediately and returns a completed future.
     * - Otherwise, queues via [Minecraft.submit] and returns a pending future.
     */
    fun <T> scheduleOnRenderThread(
        setupFor: Lighting.Entry? = null,
        block: () -> T,
    ): CompletableFuture<T> =
        if (RenderSystem.isOnRenderThread()) CompletableFuture.completedFuture(runOnRenderThread(setupFor, block))
        else Minecraft.getInstance().submit<T> {
            runOnRenderThread(setupFor, block)
        }

    /**
     * Used for some debugging purposes.
     */
    val absoluteTranslation
        get() = run {
            val xTranslate = 0
            val yTranslate = 0
            val zTranslate = 0
            Triple(xTranslate, yTranslate, zTranslate)
        }

    // todo move to GuiRenderUtils?
    fun Slot.highlight(color: LorenzColor) {
        highlight(color.toColor())
    }

    // TODO eventually removed awt.Color support, we should only use moulconfig.ChromaColour or LorenzColor
    fun Slot.highlight(color: Color) {
        highlight(color, x, y)
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

    private fun highlight(color: Color, x: Int, y: Int) = DrawContextUtils.pushPop {
        GuiRenderUtils.drawRect(x, y, x + 16, y + 16, color.rgb)
    }

    fun Slot.drawBorder(color: LorenzColor) {
        drawBorder(color.toColor())
    }

    fun Slot.drawBorder(color: Color) {
        drawBorder(color, x, y)
    }

    fun RenderGuiItemOverlayEvent.drawBorder(color: LorenzColor) {
        drawBorder(color.toColor())
    }

    fun RenderGuiItemOverlayEvent.drawBorder(color: Color) {
        drawBorder(color, x, y)
    }

    fun drawBorder(color: Color, x: Int, y: Int) = DrawContextUtils.pushPop {
        GuiRenderUtils.drawRect(x, y, x + 1, y + 16, color.rgb)
        GuiRenderUtils.drawRect(x, y, x + 16, y + 1, color.rgb)
        GuiRenderUtils.drawRect(x, y + 15, x + 16, y + 16, color.rgb)
        GuiRenderUtils.drawRect(x + 15, y, x + 16, y + 16, color.rgb)
    }

    fun interpolate(currentValue: Double, lastValue: Double, multiplier: Double): Double {
        return lastValue + (currentValue - lastValue) * multiplier
    }

    fun Position.transform(): Pair<Int, Int> {
        DrawContextUtils.translate(getAbsX().toFloat(), getAbsY().toFloat())
        DrawContextUtils.scale(effectiveScale, effectiveScale)
        val x = ((GuiScreenUtils.mouseX - getAbsX()) / effectiveScale).toInt()
        val y = ((GuiScreenUtils.mouseY - getAbsY()) / effectiveScale).toInt()
        return x to y
    }

    @Deprecated("Use renderRenderable instead", ReplaceWith("renderRenderable(renderable, posLabel)"))
    private fun Position.renderString0(string: String, offsetX: Int = 0, offsetY: Int = 0, centered: Boolean): Int =
        DrawContextUtils.pushPopResult {
            val display = "§f$string"
            transform()
            val fr = Minecraft.getInstance().font

            DrawContextUtils.translate(offsetX + 1.0, offsetY + 1.0)

            val finalX = if (centered) {
                offsetX - (fr.width(string) / 2f)
            } else 0f
            GuiRenderUtils.drawString(display, finalX, 0f, -1)

            return fr.width(display)
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
        renderables.forEach { line ->
            DrawContextUtils.pushPop {
                val (x, y) = transform()
                DrawContextUtils.translate(0f, longestY.toFloat())
                Renderable.withMousePosition(x, y) {
                    line.renderXAligned(0, longestY, longestX)
                }

                longestY += line.height + extraSpace + 2
            }
        }
        if (addToGuiManager) GuiEditManager.add(this, posLabel, longestX, longestY)
    }

    // TODO clean up nullable calls - the function param should not take `Renderable?`,
    //  the onus should be on the caller to check before calling this.
    fun Position.renderRenderable(
        renderable: Renderable,
        posLabel: String,
        addToGuiManager: Boolean = true,
    ) {
        // cause crashes and errors on purpose
        DrawContextUtils.drawContext
        DrawContextUtils.pushPop {
            val (x, y) = transform()
            Renderable.withMousePosition(x, y) {
                renderable.render(0, 0)
            }
        }
        if (addToGuiManager) GuiEditManager.add(this, posLabel, renderable.width, renderable.height)
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
        val fontRenderer = Minecraft.getInstance().font

        DrawContextUtils.pushPop {
            DrawContextUtils.translate((xPos - fontRenderer.width(text)).toFloat(), yPos.toFloat())
            DrawContextUtils.scale(scale, scale)
            GuiRenderUtils.drawString(text, 0f, 0f, -1)

            val reverseScale = 1 / scale

            DrawContextUtils.scale(reverseScale, reverseScale)
        }
    }
}
