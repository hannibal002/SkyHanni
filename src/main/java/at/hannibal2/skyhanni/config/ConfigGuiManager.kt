package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.SkyHanniMod
//#if TODO
import at.hannibal2.skyhanni.data.GuiEditManager
//#endif
import io.github.notenoughupdates.moulconfig.gui.GuiScreenElementWrapper
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor

// todo 1.21 impl needed
object ConfigGuiManager {

    var editor: MoulConfigEditor<Features>? = null

    fun getEditorInstance() = editor ?: MoulConfigEditor(SkyHanniMod.configManager.processor).also { editor = it }

    fun openConfigGui(search: String? = null) {
        val editor = getEditorInstance()

        if (search != null) {
            editor.search(search)
        }
        SkyHanniMod.screenToOpen = GuiScreenElementWrapper(editor)
    }

    fun onCommand(args: Array<String>) {
        if (args.isNotEmpty()) {
            if (args[0].lowercase() == "gui") {
                //#if TODO
                GuiEditManager.openGuiPositionEditor(hotkeyReminder = true)
                //#endif
            } else {
                openConfigGui(args.joinToString(" "))
            }
        } else {
            openConfigGui()
        }
    }
}
