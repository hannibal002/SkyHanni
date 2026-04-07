package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds

/**
 * Abstract base for features that render a full replacement UI on top of a Minecraft chest container.
 * Follows the same self-registration pattern as [at.hannibal2.skyhanni.utils.InventoryDetector]:
 * each instance registers with a [@SkyHanniModule][SkyHanniModule] companion that owns the event hooks.
 *
 * Subclasses must implement [buildContent] and may override [theme], [isPassthrough],
 * [configuredScale], and the lifecycle hooks.
 *
 * @param checkInventoryName Predicate that returns true when the given inventory name matches this overlay.
 */
abstract class SkyHanniContainerOverlayScreen(
    val checkInventoryName: (String) -> Boolean,
) {

    /** Live theme for this overlay. Override to read from config. */
    open val theme: SkyHanniOverlayTheme get() = SkyHanniOverlayTheme.DEFAULT

    /**
     * When true the overlay is suppressed and the raw container renders normally.
     * Override as a computed property backed by an `editMode` flag.
     */
    open val isPassthrough: Boolean get() = false

    /**
     * When false, the overlay will not activate on inventory open.
     * Override to gate on a config toggle.
     */
    open val shouldActivate: Boolean get() = true

    /** The user-configured maximum scale (0-200). Override to read from config. */
    open val configuredScale: Int get() = 100

    /** Called once when the matching inventory is detected as fully opened. */
    open fun onOverlayInit() {}

    /** Called when the inventory is fully closed (delayed 300 ms to skip page navigation). */
    open fun onOverlayClose() {}

    /**
     * Called each frame after the overlay is rendered. [renderableTopCorner] and [renderableDimensions]
     * are guaranteed to be current. Use this to render supplementary elements or poll async state.
     *
     * @param mouseX Current mouse X in screen coordinates.
     * @param mouseY Current mouse Y in screen coordinates.
     */
    open fun onOverlayDrawScreen(mouseX: Int, mouseY: Int) {}

    /**
     * Called inside the draw push-pop block after the main renderable is rendered.
     * Use this to render supplementary elements that require the translated matrix context.
     */
    open fun onAfterRender() {}

    /** Called when the SkyHanni branding label is left-clicked. */
    open fun onBrandingClick() {}

    /**
     * Returns true when a mouse-button keybind is held and the overlay should allow the raw
     * mouse click to pass through to the underlying container. Override to implement
     * mouse-driven click-through (e.g. negative LWJGL key codes).
     */
    open fun allowMouseClick(): Boolean = false

    /**
     * Returns true when a keyboard keybind is held and the overlay should allow the raw
     * key press to pass through to the underlying container. Override to implement
     * keyboard-driven click-through.
     */
    open fun allowKeyboardClick(): Boolean = false

    /** Returns the content area renderable. Chrome wrapping is applied by the base. */
    abstract fun buildContent(): Renderable

    internal var displayRenderable: Renderable? = null
    private var maxRenderedSize: Pair<Int, Int>? = null

    /** Top-left corner of the rendered overlay in screen coordinates. Updated each frame. */
    var renderableTopCorner: Pair<Int, Int> = 0 to 0
        internal set

    /** Width and height of the rendered overlay. Updated each frame. */
    var renderableDimensions: Pair<Int, Int> = 0 to 0
        internal set

    /** Current active scale after auto-fit clamping. */
    var activeScale: Int = 100
        internal set

    /** True when this overlay is the active one and not in passthrough mode. */
    val isOverlayVisible: Boolean
        get() = Companion.activeOverlay === this && !isPassthrough

    /** Rebuilds the chrome-wrapped display renderable. Call when content structure changes. */
    fun rebuildDisplay() {
        displayRenderable = wrapWithBackground(buildContent())
    }

    /**
     * Clears the cached max-rendered size so the next frame recomputes auto-scaling from scratch.
     * Call from config-change observers that affect the overlay layout.
     */
    fun invalidateScale() {
        maxRenderedSize = null
    }

    private fun wrapWithBackground(content: Renderable): Renderable {
        val t = theme
        val brandingScale = 1.0 * (activeScale / 100.0)
        val brandingText = Renderable.text(
            "§7SkyHanni",
            brandingScale,
            horizontalAlign = HorizontalAlignment.RIGHT,
            verticalAlign = VerticalAlignment.BOTTOM,
        )
        return Renderable.drawInsideRoundedRect(
            Renderable.doubleLayered(
                content,
                Renderable.clickable(
                    Renderable.hoverable(
                        hovered = Renderable.underlined(brandingText),
                        unHovered = brandingText,
                    ),
                    onLeftClick = { onBrandingClick() },
                ),
                blockBottomHover = false,
            ),
            color = t.backgroundColor,
            padding = (t.backgroundPadding * (activeScale / 100.0)).toInt(),
            radius = t.borderRadius,
        )
    }

    // Returns true when activeScale changed and a rebuild is needed.
    internal fun updateActiveScale(screenWidth: Int, screenHeight: Int): Boolean {
        val rendered = maxRenderedSize ?: run {
            activeScale = configuredScale
            rebuildDisplay()
            val r = displayRenderable ?: return false
            maxRenderedSize = r.width to r.height
            return false
        }
        val unscaledW = rendered.first.toDouble() / activeScale
        val unscaledH = rendered.second.toDouble() / activeScale
        val maxFitScale = min(
            0.95 * screenWidth / unscaledW,
            0.95 * screenHeight / unscaledH,
        ).toInt()
        val newScale = configuredScale.coerceAtMost(maxFitScale)
        if (newScale == activeScale) return false
        activeScale = newScale
        return true
    }

    init {
        overlays.add(this)
    }

    @SkyHanniModule
    companion object {
        private val overlays = mutableListOf<SkyHanniContainerOverlayScreen>()

        var activeOverlay: SkyHanniContainerOverlayScreen? = null
            private set

        private var lastScreenSize: Pair<Int, Int>? = null

        @HandleEvent
        fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
            val overlay = overlays.firstOrNull { it.checkInventoryName(event.inventoryName) } ?: return
            if (!overlay.shouldActivate) return
            activeOverlay = overlay
            overlay.onOverlayInit()
            // rebuildDisplay() requires the render thread (font metrics); onPreDraw rebuilds lazily if null
        }

        @HandleEvent
        fun onInventoryClose(event: InventoryCloseEvent) {
            val overlay = activeOverlay ?: return
            activeOverlay = null
            lastScreenSize = null
            DelayedRun.runDelayed(300.milliseconds) {
                if (activeOverlay === overlay) return@runDelayed
                overlay.onOverlayClose()
                overlay.displayRenderable = null
                overlay.maxRenderedSize = null
            }
        }

        @HandleEvent(onlyOnSkyblock = true)
        fun onPreDraw(event: GuiContainerEvent.PreDraw) {
            val overlay = activeOverlay ?: return
            val gui = event.gui

            if (overlay.isPassthrough) {
                overlay.onOverlayDrawScreen(event.mouseX, event.mouseY)
                return
            }

            val screenSize = gui.width to gui.height
            if (screenSize != lastScreenSize) {
                lastScreenSize = screenSize
                if (overlay.updateActiveScale(gui.width, gui.height)) {
                    overlay.rebuildDisplay()
                }
            }

            val renderable = overlay.displayRenderable ?: run {
                overlay.rebuildDisplay()
                overlay.displayRenderable ?: return
            }

            val left = (gui.width - renderable.width) / 2
            val top = (gui.height - renderable.height) / 2
            overlay.renderableTopCorner = left to top
            overlay.renderableDimensions = renderable.width to renderable.height

            DrawContextUtils.pushPop {
                DrawContextUtils.translate(left.toFloat(), top.toFloat())
                Renderable.withMousePosition(event.mouseX - left, event.mouseY - top) {
                    renderable.render(0, 0)
                }
                overlay.onAfterRender()
            }

            // Called after rendering so renderableTopCorner/Dimensions are already set.
            overlay.onOverlayDrawScreen(event.mouseX, event.mouseY)
            event.cancel()
        }
    }
}
