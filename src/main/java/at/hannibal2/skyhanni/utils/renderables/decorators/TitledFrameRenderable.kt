package at.hannibal2.skyhanni.utils.renderables.decorators

import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.render.ShaderRenderUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text

/**
 * Draws a rounded, translucent card around [root] with [title] as a heading, separated from the
 * content by a thin accent rule.
 */
class TitledFrameRenderable(
    override val root: Renderable,
    private val title: Renderable,
    private val accentColor: Int = DEFAULT_ACCENT,
    private val backgroundColor: Int = DEFAULT_BACKGROUND,
    private val padding: Int = DEFAULT_PADDING,
    override val horizontalAlign: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.LEFT,
    override val verticalAlign: RenderUtils.VerticalAlignment = RenderUtils.VerticalAlignment.TOP,
) : RenderableDecorator {

    private val titleHeight get() = title.height

    /** Where the content starts, below the heading and its rule. */
    private val contentTop get() = padding + titleHeight + TITLE_GAP

    override val width: Int
        get() = maxOf(root.width, title.width) + padding * 2

    override val height: Int get() = contentTop + root.height + padding

    override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
        val w = width
        val h = height

        ShaderRenderUtils.drawRoundRectDeferred(0, 0, w, h, backgroundColor, RADIUS, SMOOTHNESS)
        ShaderRenderUtils.drawRoundRectOutlineDeferred(
            0, 0, w, h,
            accentColor,
            fadeOut(accentColor),
            BORDER_THICKNESS,
            RADIUS,
            BLUR,
        )

        DrawContextUtils.pushPop {
            DrawContextUtils.translate(padding.toFloat(), padding.toFloat())
            title.render(mouseOffsetX + padding, mouseOffsetY + padding)
        }

        // Thin rule between heading and content, inset so it does not touch the rounded corners.
        val ruleY = padding + titleHeight + RULE_OFFSET
        GuiRenderUtils.drawRect(padding, ruleY, w - padding, ruleY + 1, fadeOut(accentColor))

        DrawContextUtils.pushPop {
            DrawContextUtils.translate(padding.toFloat(), contentTop.toFloat())
            root.render(mouseOffsetX + padding, mouseOffsetY + contentTop)
        }
    }

    /** Same hue at a much lower alpha, used for the softer parts of the frame. */
    private fun fadeOut(color: Int) = (color and 0x00FFFFFF) or (FADED_ALPHA shl 24)

    companion object {
        private const val RADIUS = 6
        private const val SMOOTHNESS = 1f
        private const val BLUR = 0.4f
        private const val BORDER_THICKNESS = 1
        private const val DEFAULT_PADDING = 8

        /** Vertical space between the heading and the first content line. */
        private const val TITLE_GAP = 7
        private const val RULE_OFFSET = 3
        private const val FADED_ALPHA = 0x55

        private const val DEFAULT_ACCENT = 0xFFD070FF.toInt()
        private const val DEFAULT_BACKGROUND = 0xB0101014.toInt()

        fun Renderable.withTitledFrame(
            title: Renderable,
            accentColor: Int = DEFAULT_ACCENT,
            backgroundColor: Int = DEFAULT_BACKGROUND,
            padding: Int = DEFAULT_PADDING,
        ) = TitledFrameRenderable(this, title, accentColor, backgroundColor, padding)

        fun Renderable.withTitledFrame(
            title: String,
            accentColor: Int = DEFAULT_ACCENT,
            backgroundColor: Int = DEFAULT_BACKGROUND,
            padding: Int = DEFAULT_PADDING,
        ) = withTitledFrame(Renderable.text(title), accentColor, backgroundColor, padding)
    }
}
