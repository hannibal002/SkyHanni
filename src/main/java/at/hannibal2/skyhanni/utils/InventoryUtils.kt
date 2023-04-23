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
        } else ""
        return chestName
    }
    
    fun ContainerChest.getInventoryName() = this.lowerChestInventory.displayName.unformattedText.trim()

    fun getItemsInOwnInventory(): MutableList<ItemStack> {
        val list = mutableListOf<ItemStack>()
        for (itemStack in Minecraft.getMinecraft().thePlayer.inventory.mainInventory) {
            itemStack?.let {
                list.add(it)
            }
        }

        return list
    }

    fun countItemsInLowerInventory(predicate: (ItemStack) -> Boolean) =
        getItemsInOwnInventory().filter { predicate(it) }.sumOf { it.stackSize }

    fun getArmor(): Array<ItemStack?> =
        Minecraft.getMinecraft().thePlayer.inventory.armorInventory

    fun inStorage() = openInventoryName().let { it.contains("Storage") || it.contains("Ender Chest") || it.contains("Backpack") }
}