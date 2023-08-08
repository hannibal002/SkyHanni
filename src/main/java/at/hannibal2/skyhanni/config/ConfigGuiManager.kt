package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.SkyHanniMod
import io.github.moulberry.moulconfig.gui.GuiScreenElementWrapper
import io.github.moulberry.moulconfig.gui.MoulConfigEditor

object ConfigGuiManager {
    val editor by lazy { MoulConfigEditor(SkyHanniMod.configManager.processor) }

    fun openConfigGui(search: String? = null) {
        if (search != null) {
            editor.search(search)
        }
        SkyHanniMod.screenToOpen = GuiScreenElementWrapper(editor)
    }


}