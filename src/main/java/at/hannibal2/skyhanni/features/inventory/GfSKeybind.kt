package at.hannibal2.hanni.features.inventory

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.GetFromSackApi
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.GuiKeyPressEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.hanni.utils.KeyboardManager.isKeyClicked
import at.hannibal2.hanni.utils.compat.stackUnderCursor

@HanniModule
object GfSKeybind {
    private val config get() = HanniMod.feature.inventory.gfs

    @HandleEvent
    fun onKey(event: GuiKeyPressEvent) {
        if (!config.keybind.isKeyClicked()) return
        stackUnderCursor()?.getInternalNameOrNull()?.let {
            GetFromSackApi.getFromSack(it, 9999)
        }
    }
}
