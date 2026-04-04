package at.hannibal2.skyhanni.features.pets

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.pets.display.PetDisplayConfig
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import io.github.notenoughupdates.moulconfig.gui.GuiElementComponent
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent
import io.github.notenoughupdates.moulconfig.processor.BuiltinMoulConfigGuis
import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor
import net.minecraft.client.Minecraft

@SkyHanniModule
object PetDisplayConfigGuiManager {

    private var editor: MoulConfigEditor<PetDisplayConfig>? = null

    fun getEditorInstance(): MoulConfigEditor<PetDisplayConfig> {
        editor?.let { return it }
        val config = SkyHanniMod.feature.misc.pets.display
        val processor = MoulConfigProcessor(config)
        BuiltinMoulConfigGuis.addProcessors(processor)
        val driver = ConfigProcessorDriver(processor)
        driver.warnForPrivateFields = false
        driver.processConfig(config)
        return MoulConfigEditor(processor).apply {
            wide = true
        }.also { editor = it }
    }

    fun open() {
        ConfigUtils.openEditor(getEditorInstance())
    }

    fun isOpen(): Boolean {
        val currentEditor = editor ?: return false
        val screen = Minecraft.getInstance().screen as? MoulConfigScreenComponent ?: return false
        val root = screen.guiContext.root as? GuiElementComponent ?: return false
        return root.element === currentEditor
    }

    /**
     * Renders the pet display preview in a MoulConfig-styled pane positioned below the editor window.
     * The pane provides an opaque background that prevents MoulConfig's blur from showing through
     * the semi-transparent circular backgrounds in the pet display.
     *
     * @param renderable The built pet display renderable to preview.
     */
    fun renderPreview(renderable: Renderable) {
        val window = Minecraft.getInstance().window
        val screenWidth = window.guiScaledWidth
        val screenHeight = window.guiScaledHeight
        val margin = 100 / window.guiScale.coerceAtLeast(1)
        val ySize = (screenHeight - margin).coerceAtMost(400)
        val editorBottom = (screenHeight + ySize) / 2

        val paneWidth = renderable.width + INNER_PAD * 2
        val paneHeight = renderable.height + INNER_PAD * 2 + LABEL_HEIGHT + LABEL_GAP
        val paneLeft = (screenWidth - paneWidth) / 2
        val paneRight = paneLeft + paneWidth
        val paneBottom = editorBottom + paneHeight

        // Shadow (sides and bottom only, top connects flush to the editor)
        val shadowRight = paneRight + SHADOW_SIZE
        val shadowBottom = paneBottom + SHADOW_SIZE
        GuiRenderUtils.drawRect(paneLeft - SHADOW_SIZE, editorBottom, paneLeft, shadowBottom, SHADOW_COLOR)
        GuiRenderUtils.drawRect(paneRight, editorBottom, shadowRight, shadowBottom, SHADOW_COLOR)
        GuiRenderUtils.drawRect(paneLeft - SHADOW_SIZE, paneBottom, shadowRight, shadowBottom, SHADOW_COLOR)

        // Outer border (1px; top edge connects to editor bottom)
        GuiRenderUtils.drawRect(paneLeft - 1, editorBottom - 1, paneRight + 1, paneBottom + 1, BORDER_COLOR)
        // Inner background
        GuiRenderUtils.drawRect(paneLeft, editorBottom, paneRight, paneBottom, BACKGROUND_COLOR)

        val label = "Preview"
        val fr = Minecraft.getInstance().font
        val labelX = paneLeft + (paneWidth - fr.width(label)) / 2
        GuiRenderUtils.drawString(label, labelX, editorBottom + INNER_PAD / 2, LABEL_COLOR, shadow = false)

        val renderX = paneLeft + INNER_PAD
        val renderY = editorBottom + INNER_PAD + LABEL_HEIGHT + LABEL_GAP
        DrawContextUtils.pushPop {
            DrawContextUtils.translate(renderX.toFloat(), renderY.toFloat())
            Renderable.withMousePosition(renderX, renderY) { renderable.render(0, 0) }
        }
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
    }

    @HandleEvent(ConfigLoadEvent::class)
    fun onConfigLoad() {
        invalidate()
    }
}
