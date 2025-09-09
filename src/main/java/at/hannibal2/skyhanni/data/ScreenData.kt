package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.client.MinecraftClient

@SkyHanniModule
object ScreenData {
    private var wasOpen = false

    @HandleEvent
    fun onTick() {
        val isOpen = MinecraftClient.getInstance().currentScreen != null
        if (wasOpen == isOpen) return
        wasOpen = isOpen
        if (!wasOpen) {
            InventoryCloseEvent("?", false).post()
        }
    }
}
