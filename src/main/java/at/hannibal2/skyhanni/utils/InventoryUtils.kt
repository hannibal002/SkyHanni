package at.hannibal2.skyhanni.utils

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack

object InventoryUtils {

    //TODO use this method more widely
    fun currentlyOpenInventory(): String {
        val screen = Minecraft.getMinecraft().currentScreen
        if (screen !is GuiChest) return ""
        val inventorySlots = screen.inventorySlots
        val chest = inventorySlots as ContainerChest

        return chest.getInventoryName()
    }

    fun getItemsInOpenChest(): List<Slot> {
        val list = mutableListOf<Slot>()
        val guiChest = Minecraft.getMinecraft().currentScreen as GuiChest
        val inventorySlots = guiChest.inventorySlots.inventorySlots
        val skipAt = inventorySlots.size - 9 * 4
        var i = 0
        for (slot in inventorySlots) {
            val stack = slot.stack
            if (stack != null) {
                list.add(slot)
            }
            i++
            if (i == skipAt) break
        }
        return list
    }

    fun openInventoryName(): String {
        val guiChest = Minecraft.getMinecraft().currentScreen
        val chestName = if (guiChest is GuiChest) {
            val chest = guiChest.inventorySlots as ContainerChest
            chest.getInventoryName()
        } else {
            ""
        }
        return chestName
    }
    
    fun ContainerChest.getInventoryName(): String {
        return this.lowerChestInventory.displayName.unformattedText.trim()
    }

    fun countItemsInLowerInventory(predicate: (ItemStack) -> Boolean) =
        Minecraft.getMinecraft().thePlayer.inventory.mainInventory
            .filter { it != null && predicate(it) }
            .sumOf { it.stackSize }
}