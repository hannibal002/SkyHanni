package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.text

/**
 * Abstract base for SkyHanni content screens.
 *
 * Subclasses provide [title] and [buildContent]. The dark chrome (shadow, rounded
 * panel, title bar with separator) is rendered automatically. Call [rebuildDisplay]
 * whenever the content structure changes (e.g., mode switch, list mutation).
 *
 * @see SkyHanniTabScreen for tab-navigated screens.
 * @see SkyHanniCanvasScreen for full-canvas screens without chrome.
 */
abstract class SkyHanniChromeScreen : SkyHanniBaseScreen() {

    abstract val screenTitle: String

    /** Returns the Renderable representing this screen's content area. */
    abstract fun buildContent(): Renderable

    private var display: Renderable? = null
    private var lastWidth = 0
    private var lastHeight = 0

    /**
     * Rebuilds the full display Renderable (chrome + content).
     * Call this whenever the content structure changes.
     */
    fun rebuildDisplay() {
        display = buildChrome(buildContent())
    }

    private fun buildChrome(content: Renderable): Renderable {
        val titleText = Renderable.text("§l$screenTitle")
        val separatorInt = SkyHanniScreenTheme.SEPARATOR_INT
        val panelWidth = content.width.coerceAtLeast(titleText.width)
        val separator = object : Renderable {
            override val width = panelWidth
            override val height = 1
            override val horizontalAlign = HorizontalAlignment.LEFT
            override val verticalAlign = VerticalAlignment.TOP
            override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
                GuiRenderUtils.drawRect(0, 0, width, 1, separatorInt)
            }
        }
        val body = Renderable.vertical(
            listOf(titleText, separator, content),
            spacing = 4,
        )
        return Renderable.drawInsideFloatingRectWithBorder(
            body,
            backgroundColor = SkyHanniScreenTheme.COLOR_BG,
            lightColor = SkyHanniScreenTheme.COLOR_BORDER_TOP,
            darkColor = SkyHanniScreenTheme.COLOR_BORDER_BOT,
            padding = SkyHanniScreenTheme.PANEL_PADDING,
            radius = SkyHanniScreenTheme.PANEL_RADIUS,
            borderThickness = SkyHanniScreenTheme.PANEL_BORDER,
        )
    }

    final override fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground(mouseX, mouseY, partialTicks)
        if (lastWidth != width || lastHeight != height) {
            lastWidth = width
            lastHeight = height
            rebuildDisplay()
        }
        val panel = display ?: return
        val startX = (width - panel.width) / 2
        val startY = (height - panel.height) / 2

        val shadow = SkyHanniScreenTheme.SHADOW_INT
        val o1 = SkyHanniScreenTheme.SHADOW_OFFSET_1
        val o2 = SkyHanniScreenTheme.SHADOW_OFFSET_2
        GuiRenderUtils.drawRect(startX + o1, startY + o1, startX + panel.width + o1, startY + panel.height + o1, shadow)
        GuiRenderUtils.drawRect(startX + o2, startY + o2, startX + panel.width + o2, startY + panel.height + o2, shadow)

        DrawContextUtils.pushPop {
            DrawContextUtils.translate(startX.toFloat(), startY.toFloat())
            Renderable.withMousePosition(mouseX - startX, mouseY - startY) {
                panel.render(0, 0)
            }
        }
    }

    final override fun onInitGui() {
        rebuildDisplay()
        onChromeInit()
    }

    /** Called after the screen initializes. Override instead of [onInitGui]. */
    open fun onChromeInit() {}
}
