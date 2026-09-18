package at.hannibal2.skyhanni.config.core.dependency

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.ConfigJumpHighlight
import at.hannibal2.skyhanni.utils.ConfigUtils.asStructuredText
import io.github.notenoughupdates.moulconfig.common.IMinecraft
import io.github.notenoughupdates.moulconfig.common.RenderContext
import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor
import io.github.notenoughupdates.moulconfig.gui.KeyboardEvent
import io.github.notenoughupdates.moulconfig.gui.MouseEvent
import java.lang.reflect.Field
import kotlin.math.sin

/**
 * Decorator that shows the option name and description in a large overlay next to the cursor.
 * The overlay appears when the option row is hovered for a configurable delay, or immediately
 * while Shift is held. Also draws a temporary yellow highlight after jumping to this option
 * from elsewhere in the config.
 */
class GuiOptionEditorBigDescription(
    private val base: GuiOptionEditor,
    private val field: Field,
) : GuiOptionEditor(base.getOption()) {
    private var hoverStart = 0L
    private var wasHovered = false

    override fun render(context: RenderContext, x: Int, y: Int, width: Int) {
        // Only treat the actual option content as hoverable, not banners/panels reserved
        // above it (used-by, dependencies, third-party warnings).
        val offset = (base as? ConfigBannerProvider)?.bannerOffset() ?: 0
        val contentTop = y + offset
        val contentBottom = y + base.height
        val hovered = IMinecraft.INSTANCE.mouseX in x..(x + width) &&
            IMinecraft.INSTANCE.mouseY in contentTop until contentBottom
        val now = System.currentTimeMillis()
        if (hovered) {
            if (!wasHovered) hoverStart = now
            wasHovered = true
        } else {
            wasHovered = false
        }
        base.render(context, x, y, width)
        val remaining = ConfigJumpHighlight.remainingMs(field.declaringClass.name, field.name)
        if (remaining > 0) {
            // slightly smoothed blinking: sine oscillation over the highlight duration
            val progress = remaining.toDouble() / ConfigJumpHighlight.DURATION_MS
            val blink = 0.5 + 0.5 * sin(progress * Math.PI * 4)
            val alpha = (20 + 55 * blink).toInt().coerceIn(20, 75)
            val color = (alpha shl 24) or 0x00FFFF00
            context.drawColoredRect(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + base.height).toFloat(), color)
        }
    }

    override fun renderOverlay(context: RenderContext, x: Int, y: Int, width: Int) {
        val show = wasHovered && SkyHanniMod.feature.about.bigDescriptionEnabled &&
            (context.isShiftDown() || System.currentTimeMillis() - hoverStart > delayMs())
        if (show) drawBigDescription(context)
        base.renderOverlay(context, x, y, width)
    }

    private fun delayMs(): Long = (SkyHanniMod.feature.about.bigDescriptionDelay * 1000).toLong().coerceAtLeast(250)

    private fun drawBigDescription(context: RenderContext) {
        val description = runCatching { base.getOption().getDescription().text.trim() }.getOrNull()
            ?: return
        if (description.isEmpty()) return
        val name = runCatching { base.getOption().getName().text.trim() }.getOrNull() ?: return

        val font = IMinecraft.INSTANCE.defaultFontRenderer
        val maxLineWidth = MAX_LINE_CHARS * 5
        val lines = buildList {
            add(name)
            add("")
            description.replace("\\n", "\n").split('\n').forEach { raw ->
                if (raw.isEmpty()) {
                    add("")
                } else {
                    font.splitText(raw.asStructuredText(), maxLineWidth).forEach { add(it.text) }
                }
            }
        }
        if (lines.size > MAX_LINES) return

        val scaledLineHeight = (font.height * SCALE).toInt() + 2
        val panelW = (lines.maxOf { font.getStringWidth(it.asStructuredText()) } * SCALE).toInt() + 24
        val panelH = lines.size * scaledLineHeight + 16

        val mouseX = IMinecraft.INSTANCE.mouseX
        val mouseY = IMinecraft.INSTANCE.mouseY
        val px = mouseX + 14
        val py = mouseY + 14

        context.drawColoredRect(px.toFloat(), py.toFloat(), (px + panelW).toFloat(), (py + panelH).toFloat(), PANEL_BG)
        context.drawColoredRect(px.toFloat(), py.toFloat(), (px + panelW).toFloat(), (py + 1).toFloat(), PANEL_BORDER)
        context.drawColoredRect(px.toFloat(), (py + panelH - 1).toFloat(), (px + panelW).toFloat(), (py + panelH).toFloat(), PANEL_BORDER)
        context.drawColoredRect(px.toFloat(), py.toFloat(), (px + 1).toFloat(), (py + panelH).toFloat(), PANEL_BORDER)
        context.drawColoredRect((px + panelW - 1).toFloat(), py.toFloat(), (px + panelW).toFloat(), (py + panelH).toFloat(), PANEL_BORDER)

        lines.forEachIndexed { index, line ->
            val color = if (index == 0) NAME_COLOR else TEXT_COLOR
            context.pushMatrix()
            context.translate((px + 12).toFloat(), (py + 8 + index * scaledLineHeight).toFloat())
            context.scale(SCALE, SCALE)
            context.drawString(font, line.asStructuredText(), 0, 0, color, false)
            context.popMatrix()
        }
    }

    override fun mouseInput(
        x: Int,
        y: Int,
        width: Int,
        mouseX: Int,
        mouseY: Int,
        mouseEvent: MouseEvent?,
    ): Boolean = base.mouseInput(x, y, width, mouseX, mouseY, mouseEvent)

    override fun mouseInputOverlay(
        x: Int,
        y: Int,
        width: Int,
        mouseX: Int,
        mouseY: Int,
        mouseEvent: MouseEvent?,
    ): Boolean = base.mouseInputOverlay(x, y, width, mouseX, mouseY, mouseEvent)

    override fun keyboardInput(event: KeyboardEvent?): Boolean = base.keyboardInput(event)

    override fun getHeight(): Int = base.height

    companion object {
        private const val MAX_LINE_CHARS = 48
        private const val MAX_LINES = 24
        private const val SCALE = 1.5f
        private const val PANEL_BG = 0xE01A1A2A.toInt()
        private const val PANEL_BORDER = 0xFF6464A0.toInt()
        private const val NAME_COLOR = 0xFF55FFFF.toInt()
        private const val TEXT_COLOR = -0x1
    }
}
