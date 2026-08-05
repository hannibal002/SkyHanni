package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.GetFromSackApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiKeyPressEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyClicked

@SkyHanniModule
object GfSKeybind {
    private val config get() = SkyHanniMod.feature.inventory.gfs

    @HandleEvent
    private fun onGuiKeyPress(event: GuiKeyPressEvent) {
        if (!config.keybind.isKeyClicked()) return
        event.stackUnderCursor()?.getInternalNameOrNull()?.let {
            GetFromSackApi.getFromSack(it, 9999)
        }
    }
}
