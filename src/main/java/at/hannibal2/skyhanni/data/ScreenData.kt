package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object ScreenData {
    private var wasOpen = false

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        val isOpen = Minecraft.getMinecraft().currentScreen != null
        if (wasOpen == isOpen) return
        wasOpen = isOpen
        if (!wasOpen) {
            InventoryCloseEvent(false).postAndCatch()
        }
    }
}
