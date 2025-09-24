package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.GetFromSackApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiKeyPressEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyClicked
import at.hannibal2.skyhanni.utils.compat.stackUnderCursor

@SkyHanniModule
object GfSKeybind {
    private val config get() = SkyHanniMod.feature.inventory.gfs

    @HandleEvent
    fun onKey(event: GuiKeyPressEvent) {
        if (!config.keybind.isKeyClicked()) return
        stackUnderCursor()?.getInternalNameOrNull()?.let {
            GetFromSackApi.getFromSack(it, 9999)
        }
    }
}
