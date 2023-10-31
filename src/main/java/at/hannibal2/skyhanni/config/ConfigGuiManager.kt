package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.SkyHanniMod
import io.github.moulberry.moulconfig.gui.GuiScreenElementWrapper
import io.github.moulberry.moulconfig.gui.MoulConfigEditor

object ConfigGuiManager {

    var editor: MoulConfigEditor<Features>? = null

    fun openConfigGui(search: String? = null) {
        if (editor == null) {
            editor = MoulConfigEditor(SkyHanniMod.configManager.processor)
        }
        val editor = editor ?: return

        if (search != null) {
            editor.search(search)
        }
        SkyHanniMod.screenToOpen = GuiScreenElementWrapper(editor)
    }

}
