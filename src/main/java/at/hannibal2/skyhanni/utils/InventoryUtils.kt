package at.hannibal2.skyhanni.utils

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraft.item.ItemStack

object InventoryUtils {

    fun getItemsInOpenChest() = buildList {
        val guiChest = Minecraft.getMinecraft().currentScreen as GuiChest
        val inventorySlots = guiChest.inventorySlots.inventorySlots
        val skipAt = inventorySlots.size - 9 * 4
        var i = 0
        for (slot in inventorySlots) {
            val stack = slot.stack
            if (stack != null) {
                add(slot)
            }
            i++
            if (i == skipAt) break
        }
    }

    fun getItemsInChest(guiChest: GuiChest) = buildList {
        val inventorySlots = guiChest.inventorySlots.inventorySlots
        val skipAt = inventorySlots.size - 9 * 4
        var i = 0
        for (slot in inventorySlots) {
            val stack = slot.stack
            if (stack != null) {
                add(slot)
            }
            i++
            if (i == skipAt) break
        }
    }

    fun openInventoryName() = Minecraft.getMinecraft().currentScreen.let {
         if (it is GuiChest) {
             val chest = it.inventorySlots as ContainerChest
             chest.getInventoryName()
         } else ""
     }

    fun ContainerChest.getInventoryName() = this.lowerChestInventory.displayName.unformattedText.trim()

    fun getItemsInOwnInventory() = Minecraft.getMinecraft().thePlayer.inventory.mainInventory.filterNotNull()

    fun countItemsInLowerInventory(predicate: (ItemStack) -> Boolean) =
        getItemsInOwnInventory().filter { predicate(it) }.sumOf { it.stackSize }

    fun getArmor(): Array<ItemStack?> = Minecraft.getMinecraft().thePlayer.inventory.armorInventory

    fun inStorage() =
        openInventoryName().let { it.contains("Storage") || it.contains("Ender Chest") || it.contains("Backpack") }

    fun getItemInHand(): ItemStack? = Minecraft.getMinecraft().thePlayer.heldItem
}