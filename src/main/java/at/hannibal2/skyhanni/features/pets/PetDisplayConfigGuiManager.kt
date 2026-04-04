package at.hannibal2.skyhanni.features.pets

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.pets.display.PetDisplayConfig
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConfigUtils
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
        return MoulConfigEditor(processor).also { editor = it }
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

    fun invalidate() {
        editor = null
    }

    @HandleEvent(ConfigLoadEvent::class)
    fun onConfigLoad() {
        invalidate()
    }
}
