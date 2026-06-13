package at.hannibal2.skyhanni.features.pets

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.pets.display.PetDisplayConfig
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import at.hannibal2.skyhanni.utils.compat.MouseCompat
import at.hannibal2.skyhanni.utils.renderables.Renderable
import io.github.notenoughupdates.moulconfig.gui.GuiElementComponent
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent
import io.github.notenoughupdates.moulconfig.processor.BuiltinMoulConfigGuis
import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor
import net.minecraft.client.Minecraft
import kotlin.math.roundToInt

@SkyHanniModule
object PetDisplayConfigGuiManager {

    private var editor: MoulConfigEditor<PetDisplayConfig>? = null
    private val config get() = SkyHanniMod.feature.misc.pets.display
    private val widenConfig get() = SkyHanniMod.feature.gui.widenConfig

    private var previewPosition: PreviewPosition? = null
    private var draggingPreview = false
    private var dragOffsetX = 0
    private var dragOffsetY = 0
    private var wasMouseDown = false

    fun getEditorInstance(): MoulConfigEditor<PetDisplayConfig> {
        editor?.let { return it }
        val processor = MoulConfigProcessor(config)
        BuiltinMoulConfigGuis.addProcessors(processor)
        val driver = ConfigProcessorDriver(processor)
        driver.warnForPrivateFields = false
        driver.processConfig(config)
        return MoulConfigEditor(processor).apply {
            wide = widenConfig.get()
        }.also { editor = it }
    }

    fun open() {
        resetPreviewInteraction()
        ConfigUtils.openEditor(getEditorInstance())
    }

    fun isOpen(): Boolean {
        val currentEditor = editor ?: return false
        val screen = Minecraft.getInstance().screen as? MoulConfigScreenComponent ?: return false
        val root = screen.guiContext.root as? GuiElementComponent ?: return false
        return root.element === currentEditor
    }

    fun renderPreview(renderable: Renderable) {
        val window = Minecraft.getInstance().window
        val screenWidth = window.guiScaledWidth
        val screenHeight = window.guiScaledHeight
        val margin = 100 / window.guiScale.coerceAtLeast(1)
        val ySize = (screenHeight - margin).coerceAtMost(400)
        val editorBottom = (screenHeight + ySize) / 2
        val previewScale = config.preview.scale.get()
        val previewWidth = (renderable.width * previewScale).roundToInt()
        val previewHeight = (renderable.height * previewScale).roundToInt()

        val paneWidth = previewWidth + INNER_PAD * 2
        val paneHeight = previewHeight + INNER_PAD * 2 + LABEL_HEIGHT + LABEL_GAP
        val defaultLeft = (screenWidth - paneWidth) / 2
        val defaultTop = editorBottom
        val position = updatePreviewPosition(
            defaultLeft,
            defaultTop,
            paneWidth,
            paneHeight,
            screenWidth,
            screenHeight,
        )
        val paneLeft = position.left
        val paneTop = position.top
        val paneRight = paneLeft + paneWidth
        val paneBottom = paneTop + paneHeight

        val shadowRight = paneRight + SHADOW_SIZE
        val shadowBottom = paneBottom + SHADOW_SIZE
        GuiRenderUtils.drawRect(paneLeft - SHADOW_SIZE, paneTop, paneLeft, shadowBottom, SHADOW_COLOR)
        GuiRenderUtils.drawRect(paneRight, paneTop, shadowRight, shadowBottom, SHADOW_COLOR)
        GuiRenderUtils.drawRect(paneLeft - SHADOW_SIZE, paneBottom, shadowRight, shadowBottom, SHADOW_COLOR)

        GuiRenderUtils.drawRect(paneLeft - 1, paneTop - 1, paneRight + 1, paneBottom + 1, BORDER_COLOR)
        GuiRenderUtils.drawRect(paneLeft, paneTop, paneRight, paneBottom, BACKGROUND_COLOR)

        val label = "Preview"
        val fr = Minecraft.getInstance().font
        val labelX = paneLeft + (paneWidth - fr.width(label)) / 2
        GuiRenderUtils.drawString(label, labelX, paneTop + INNER_PAD / 2, LABEL_COLOR, shadow = false)

        val renderX = paneLeft + INNER_PAD
        val renderY = paneTop + INNER_PAD + LABEL_HEIGHT + LABEL_GAP
        DrawContextUtils.pushPop {
            DrawContextUtils.translate(renderX.toFloat(), renderY.toFloat())
            DrawContextUtils.scale(previewScale, previewScale)
            Renderable.withMousePosition(renderX, renderY) { renderable.render(0, 0) }
        }
    }

    private fun updatePreviewPosition(
        defaultLeft: Int,
        defaultTop: Int,
        paneWidth: Int,
        paneHeight: Int,
        screenWidth: Int,
        screenHeight: Int,
    ): PreviewPosition {
        var position = (previewPosition ?: PreviewPosition(defaultLeft, defaultTop))
            .clamped(paneWidth, paneHeight, screenWidth, screenHeight)
        val (mouseX, mouseY) = GuiScreenUtils.mousePos
        val isMouseDown = MouseCompat.isButtonDown(0)
        val isHeaderHovered = mouseX in position.left..(position.left + paneWidth) &&
            mouseY in position.top..(position.top + INNER_PAD + LABEL_HEIGHT)

        if (isMouseDown && !wasMouseDown && isHeaderHovered) {
            draggingPreview = true
            dragOffsetX = mouseX - position.left
            dragOffsetY = mouseY - position.top
        } else if (!isMouseDown) {
            draggingPreview = false
        }

        if (draggingPreview) {
            position = PreviewPosition(mouseX - dragOffsetX, mouseY - dragOffsetY)
                .clamped(paneWidth, paneHeight, screenWidth, screenHeight)
        }

        wasMouseDown = isMouseDown
        previewPosition = position
        return position
    }

    private const val INNER_PAD = 14
    private const val LABEL_HEIGHT = 10
    private const val LABEL_GAP = 4
    private const val SHADOW_SIZE = 3
    private const val SHADOW_COLOR = 0x40000000
    private const val BORDER_COLOR = 0xFF202026.toInt()
    private const val BACKGROUND_COLOR = 0xFF17171D.toInt()
    private const val LABEL_COLOR = 0xFF888888.toInt()

    fun invalidate() {
        editor = null
        previewPosition = null
        resetPreviewInteraction()
    }

    @HandleEvent(ConfigLoadEvent::class)
    fun onConfigLoad() {
        invalidate()
        ConditionalUtils.onToggle(widenConfig) {
            editor?.wide = widenConfig.get()
        }
    }

    private fun resetPreviewInteraction() {
        draggingPreview = false
        wasMouseDown = false
    }

    private fun PreviewPosition.clamped(
        paneWidth: Int,
        paneHeight: Int,
        screenWidth: Int,
        screenHeight: Int,
    ): PreviewPosition = PreviewPosition(
        left.coerceInSafe(SHADOW_SIZE, screenWidth - paneWidth - SHADOW_SIZE),
        top.coerceInSafe(SHADOW_SIZE, screenHeight - paneHeight - SHADOW_SIZE),
    )

    private fun Int.coerceInSafe(minimumValue: Int, maximumValue: Int): Int =
        if (maximumValue < minimumValue) minimumValue else coerceIn(minimumValue, maximumValue)

    private data class PreviewPosition(val left: Int, val top: Int)
}
