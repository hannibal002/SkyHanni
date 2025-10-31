package at.hannibal2.hanni.config

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.ConfigLoadEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ConditionalUtils
import at.hannibal2.hanni.utils.ConfigUtils
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor

@HanniModule
object ConfigGuiManager {

    private val widenConfig get() = HanniMod.feature.gui.widenConfig

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        getEditorInstance().wide = widenConfig.get()
        ConditionalUtils.onToggle(widenConfig) {
            getEditorInstance().wide = widenConfig.get()
        }
    }

    var editor: MoulConfigEditor<Features>? = null

    fun getEditorInstance() = editor ?: MoulConfigEditor(HanniMod.configManager.processor).also { editor = it }

    fun openConfigGui(search: String? = null) {
        val editor = getEditorInstance()

        if (search != null) {
            editor.search(search)
        }
        ConfigUtils.openEditor(editor)
    }
}
