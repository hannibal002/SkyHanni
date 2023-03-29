package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.SkyHanniMod
import io.github.moulberry.moulconfig.gui.GuiScreenElementWrapper
import io.github.moulberry.moulconfig.gui.MoulConfigEditor

object ConfigGuiManager {
    val configEditor by lazy { MoulConfigEditor(SkyHanniMod.configManager.processor) }

    fun openConfigGui(search: String? = null) {
        if (search != null) {
            configEditor.search(search)
        }
        SkyHanniMod.screenToOpen = GuiScreenElementWrapper(configEditor)
    }


}