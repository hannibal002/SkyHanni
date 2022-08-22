package at.hannibal2.skyhanni.utils

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest

object InventoryUtils {

    //TODO use this method more widely
    fun currentlyOpenInventory(): String {
        val screen = Minecraft.getMinecraft().currentScreen
        if (screen !is GuiChest) return ""
        val chest = screen.inventorySlots as ContainerChest

        return chest.lowerChestInventory.displayName.unformattedText.trim()
    }

}