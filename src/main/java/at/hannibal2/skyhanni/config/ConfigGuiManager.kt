package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.SkyHanniMod
import io.github.notenoughupdates.moulconfig.gui.GuiScreenElementWrapper
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor

object ConfigGuiManager {

    var editor: MoulConfigEditor<Features>? = null

    private val replacedSearchTerms = mapOf(
        "color" to "colour",
        "armor" to "armour",
        "endermen" to "enderman",
        "enderman" to "endermen",
        "hotkey" to "keybind",
        "gray" to "grey"
    )

    fun getEditorInstance() = editor ?: MoulConfigEditor(SkyHanniMod.configManager.processor).also {
        it.setSearchFunction { optionEditor, word ->
            return@setSearchFunction optionEditor.fulfillsSearch(replacedSearchTerms[word] ?: word)
        }
        editor = it
    }

    fun openConfigGui(search: String? = null) {
        val editor = getEditorInstance()

        if (search != null) {
            editor.search(search)
        }
        SkyHanniMod.screenToOpen = GuiScreenElementWrapper(editor)
    }
}
