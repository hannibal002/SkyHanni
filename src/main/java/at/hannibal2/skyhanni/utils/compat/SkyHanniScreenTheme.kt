package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import io.github.notenoughupdates.moulconfig.ChromaColour
import java.awt.Color

object SkyHanniScreenTheme {

    // Panel chrome (ChromaColour for drawInsideFloatingRectWithBorder)
    val COLOR_BG: ChromaColour = ChromaColour.fromStaticRGB(0x0E, 0x0E, 0x12, 245)
    val COLOR_BORDER_TOP: ChromaColour = ChromaColour.fromStaticRGB(0x3A, 0x3A, 0x50, 255)
    val COLOR_BORDER_BOT: ChromaColour = ChromaColour.fromStaticRGB(0x1A, 0x1A, 0x22, 255)

    // Int colors for direct GuiRenderUtils.drawRect calls (ARGB format)
    val SHADOW_INT: Int = (100 shl 24)
    val SEPARATOR_INT: Int = (255 shl 24) or (0x1A shl 16) or (0x1A shl 8) or 0x22

    // Row backgrounds (Color for drawInsideRoundedRect)
    val COLOR_ROW_NORMAL: Color = Color(0x1C, 0x1C, 0x24, 200)
    val COLOR_ROW_HOVER: Color = Color(0x28, 0x28, 0x3A, 220)

    // Buttons (Color for drawInsideRoundedRect)
    val COLOR_BTN_NEUTRAL: Color = Color(0x22, 0x22, 0x2E, 215)
    val COLOR_BTN_PRIMARY: Color = Color(0x1A, 0x36, 0x26, 215)
    val COLOR_BTN_DANGER: Color = Color(0x36, 0x1A, 0x1A, 215)
    val COLOR_BTN_WARNING: Color = Color(0x36, 0x26, 0x0A, 215)

    // Scrollbar (ChromaColour for scrollList params)
    val COLOR_SCROLLBAR_TRACK: ChromaColour = ChromaColour.fromStaticRGB(0x2A, 0x2A, 0x3A, 255)
    val COLOR_SCROLLBAR_THUMB: ChromaColour = ChromaColour.fromStaticRGB(0x50, 0x50, 0x68, 255)

    // Spacing
    const val PANEL_PADDING = 14
    const val PANEL_RADIUS = 8
    const val PANEL_BORDER = 1
    const val SHADOW_OFFSET_1 = 3
    const val SHADOW_OFFSET_2 = 5
    const val BTN_RADIUS = 5
    const val BTN_PADDING = 5

    /**
     * Builds a standard themed button Renderable.
     *
     * @param label The button label, may contain Minecraft color codes.
     * @param color The button background color; `.brighter()` is used on hover.
     * @param onClick Called when the button is left-clicked.
     */
    fun buildButton(label: String, color: Color, onClick: () -> Unit): Renderable {
        val text = Renderable.text(label)
        return Renderable.clickable(
            Renderable.hoverable(
                Renderable.drawInsideRoundedRect(text, color.brighter(), padding = BTN_PADDING, radius = BTN_RADIUS),
                Renderable.drawInsideRoundedRect(text, color, padding = BTN_PADDING, radius = BTN_RADIUS),
                bypassChecks = true,
            ),
            onLeftClick = onClick,
            bypassChecks = true,
        )
    }
}
