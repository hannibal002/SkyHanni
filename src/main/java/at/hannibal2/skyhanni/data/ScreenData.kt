package at.hannibal2.hanni.data

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.InventoryCloseEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import net.minecraft.client.Minecraft

@HanniModule
object ScreenData {
    private var wasOpen = false

    @HandleEvent
    fun onTick() {
        val isOpen = Minecraft.getMinecraft().currentScreen != null
        if (wasOpen == isOpen) return
        wasOpen = isOpen
        if (!wasOpen) {
            InventoryCloseEvent("?", false).post()
        }
    }
}
