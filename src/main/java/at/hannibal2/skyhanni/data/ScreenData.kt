package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent


object ScreenData {
    private var isScreenOpen = false

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isScreenOpen && Minecraft.getMinecraft().currentScreen != null) {
            isScreenOpen = true
        } else if (isScreenOpen && Minecraft.getMinecraft().currentScreen == null) {
            isScreenOpen = false
            InventoryCloseEvent(false).postAndCatch()
        }
    }
}
