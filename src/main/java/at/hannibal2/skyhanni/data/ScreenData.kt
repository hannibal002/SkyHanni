package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.SkyhanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.client.Minecraft
import at.hannibal2.skyhanni.api.event.HandleEvent

@SkyHanniModule
object ScreenData {
    private var wasOpen = false

    @HandleEvent
    fun onTick(event: SkyhanniTickEvent) {
        val isOpen = Minecraft.getMinecraft().currentScreen != null
        if (wasOpen == isOpen) return
        wasOpen = isOpen
        if (!wasOpen) {
            InventoryCloseEvent(false).post()
        }
    }
}
