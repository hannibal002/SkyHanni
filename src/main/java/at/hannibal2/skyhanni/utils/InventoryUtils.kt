package at.hannibal2.skyhanni.utils

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.ContainerChest
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import kotlin.time.Duration.Companion.seconds

object InventoryUtils {
    var itemInHandId = NEUInternalName.NONE
    var recentItemsInHand = mutableMapOf<Long, NEUInternalName>()
    var latestItemInHand: ItemStack? = null

    fun getItemsInOpenChest() = buildList<Slot> {
        val guiChest = Minecraft.getMinecraft().currentScreen as? GuiChest ?: return emptyList<Slot>()
        for (slot in guiChest.inventorySlots.inventorySlots) {
            if (slot.inventory is InventoryPlayer) break
            if (slot.stack != null) add(slot)
        }
    }

    // TODO add cache that persists until the next gui/window open/close packet is sent/received
    fun openInventoryName() = Minecraft.getMinecraft().currentScreen.let {
        if (it is GuiChest) {
            val chest = it.inventorySlots as ContainerChest
            chest.getInventoryName()
        } else ""
    }

    fun ContainerChest.getInventoryName() = this.lowerChestInventory.displayName.unformattedText.trim()

    fun getItemsInOwnInventory() = Minecraft.getMinecraft().thePlayer.inventory.mainInventory.filterNotNull()
    fun getItemsInOwnInventoryWithNull() = Minecraft.getMinecraft().thePlayer.inventory.mainInventory

    fun countItemsInLowerInventory(predicate: (ItemStack) -> Boolean) =
        getItemsInOwnInventory().filter { predicate(it) }.sumOf { it.stackSize }

    fun inStorage() = openInventoryName().let {
        (it.contains("Storage") && !it.contains("Rift Storage"))
                || it.contains("Ender Chest") || it.contains("Backpack")
    }

    fun getItemInHand(): ItemStack? = Minecraft.getMinecraft().thePlayer.heldItem

    fun getArmor(): Array<ItemStack?> = Minecraft.getMinecraft().thePlayer.inventory.armorInventory

    fun getHelmet(): ItemStack? = getArmor()[3]
    fun getChestplate(): ItemStack? = getArmor()[2]
    fun getLeggings(): ItemStack? = getArmor()[1]
    fun getBoots(): ItemStack? = getArmor()[0]


    val isNeuStorageEnabled = RecalculatingValue(10.seconds) {
//        try {
//            val config = NotEnoughUpdates.INSTANCE.config
//
//            val storageField = config.javaClass.getDeclaredField("storageGUI")
//            val storage = storageField.get(config)
//
//            val booleanField = storage.javaClass.getDeclaredField("enableStorageGUI3")
//            booleanField.get(storage) as Boolean
//        } catch (e: Throwable) {
//            ErrorManager.logError(e, "Could not read NEU config to determine if the neu storage is emabled.")
            false
//        }
    }

    fun isSlotInPlayerInventory(itemStack: ItemStack): Boolean {
        val screen = Minecraft.getMinecraft().currentScreen as? GuiContainer ?: return false
        return screen.slotUnderMouse.inventory is InventoryPlayer && screen.slotUnderMouse.stack == itemStack
    }
}
