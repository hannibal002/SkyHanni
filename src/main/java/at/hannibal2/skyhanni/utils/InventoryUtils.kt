package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.EntityUtils.getArmorInventory
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.compat.InventoryCompat
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.clickInventorySlot
import at.hannibal2.skyhanni.utils.compat.containerSlots
import at.hannibal2.skyhanni.utils.compat.normalizeAsArray
import at.hannibal2.skyhanni.utils.compat.slotUnderCursor
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import io.github.moulberry.notenoughupdates.NotEnoughUpdates
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.player.inventory.ContainerLocalMenu
import net.minecraft.client.resources.I18n
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.ContainerChest
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import kotlin.time.Duration.Companion.seconds

@Suppress("TooManyFunctions", "Unused", "MemberVisibilityCanBePrivate")
object InventoryUtils {

    var itemInHandId = NeuInternalName.NONE
    var recentItemsInHand = mutableMapOf<Long, NeuInternalName>()
    var latestItemInHand: ItemStack? = null
    private val normalChestInternalNames = setOf("container.chest", "container.chestDouble")

    fun getItemsInOpenChest(): List<Slot> {
        return getItemsInOpenChestWithNull().filter { it.stack != null }
    }

    fun getItemsInOpenChestWithNull(): List<Slot> {
        val guiChest = Minecraft.getMinecraft().currentScreen as? GuiChest ?: return emptyList()
        return guiChest.containerSlots()
            .filter { it.inventory !is InventoryPlayer }
    }

//     fun getItemsInLowerChestWithNull(): List<Slot> {
//         val guiChest = Minecraft.getMinecraft().currentScreen as? GuiChest ?: return emptyList()
//         return guiChest.inventorySlots.inventorySlots
//             .filter { it.inventory is InventoryPlayer }
//     }

    // only works while not in an inventory
    fun getSlotsInOwnInventory(): List<Slot> {
        val guiInventory = Minecraft.getMinecraft().currentScreen as? GuiContainer ?: return emptyList()
        return guiInventory.containerSlots()
            .filter { it.inventory is InventoryPlayer && it.stack != null }
    }

    fun openInventoryName(): String = InventoryCompat.getOpenChestName()

    fun inInventory() = Minecraft.getMinecraft().currentScreen is GuiChest

    fun inContainer() = Minecraft.getMinecraft().currentScreen is GuiContainer

    fun ContainerChest.getInventoryName() = this.lowerChestInventory.displayName.unformattedText.trim()

    fun getItemsInOwnInventory(): List<ItemStack> =
        getItemsInOwnInventoryWithNull()?.filterNotNull().orEmpty()

    fun getItemsInOwnInventoryWithNull(): Array<ItemStack?>? =
        MinecraftCompat.localPlayerOrNull?.inventory?.mainInventory?.normalizeAsArray()

    // TODO use this instead of getItemsInOwnInventory() for many cases, e.g. vermin tracker, diana spade, etc
    fun getItemsInHotbar(): List<ItemStack> =
        getItemsInOwnInventoryWithNull()?.slice(0..8)?.filterNotNull().orEmpty()

    fun containsInLowerInventory(predicate: (ItemStack) -> Boolean): Boolean =
        countItemsInLowerInventory(predicate) > 0

    fun countItemsInLowerInventory(predicate: (ItemStack) -> Boolean): Int =
        getItemsInOwnInventory().filter { predicate(it) }.sumOf { it.stackSize }

    fun inStorage() = openInventoryName().let {
        (it.contains("Storage") && !it.contains("Rift Storage")) ||
            it.contains("Ender Chest") || it.contains("Backpack")
    }

    fun getItemInHand(): ItemStack? = MinecraftCompat.localPlayerOrNull?.heldItem

    fun getArmor(): Array<ItemStack?> = MinecraftCompat.localPlayerOrNull?.getArmorInventory() ?: arrayOfNulls(4)

    fun getHelmet(): ItemStack? = getArmor()[3]
    fun getChestplate(): ItemStack? = getArmor()[2]
    fun getLeggings(): ItemStack? = getArmor()[1]
    fun getBoots(): ItemStack? = getArmor()[0]

    fun GuiContainerEvent.SlotClickEvent.makeShiftClick() {
        if (this.clickedButton == 1 && slot?.stack?.getItemCategoryOrNull() == ItemCategory.SACK) return
        slot?.slotNumber?.let { slotNumber ->
            clickInventorySlot(slotNumber, container.windowId, 0, 1)
            this.cancel()
        }
    }

    val isNeuStorageEnabled by RecalculatingValue(10.seconds) {
        if (!PlatformUtils.isNeuLoaded()) {
            return@RecalculatingValue false
        }
        try {
            val config = NotEnoughUpdates.INSTANCE.config

            val storageField = config.javaClass.getDeclaredField("storageGUI")
            val storage = storageField[config]

            val booleanField = storage.javaClass.getDeclaredField("enableStorageGUI3")
            booleanField[storage] as Boolean
        } catch (e: Throwable) {
            ErrorManager.logErrorWithData(e, "Could not read NEU config to determine if the neu storage is enabled.")
            false
        }
    }

    fun isSlotInPlayerInventory(itemStack: ItemStack): Boolean {
        val slotUnderMouse = slotUnderCursor() ?: return false
        return slotUnderMouse.inventory is InventoryPlayer && slotUnderMouse.stack == itemStack
    }

    fun isItemInInventory(name: NeuInternalName) = name.getAmountInInventory() > 0

    fun ContainerChest.getUpperItems(): Map<Slot, ItemStack> = buildMap {
        for ((slot, stack) in getAllItems()) {
            if (slot.slotNumber != slot.slotIndex) continue
            this[slot] = stack
        }
    }

    fun ContainerChest.getLowerItems(): Map<Slot, ItemStack> = buildMap {
        for ((slot, stack) in getAllItems()) {
            if (slot.slotNumber == slot.slotIndex) continue
            this[slot] = stack
        }
    }

    fun ContainerChest.getAllItems(): Map<Slot, ItemStack> = buildMap {
        for (slot in inventorySlots) {
            if (slot == null) continue
            val stack = slot.stack ?: continue
            this[slot] = stack
        }
    }

    fun ContainerChest.getAllSlots(): Map<Slot, ItemStack?> = buildMap {
        for (slot in inventorySlots) {
            if (slot == null) continue
            this[slot] = slot.stack
        }
    }

    fun getItemAtSlotIndex(slotIndex: Int): ItemStack? = getSlotAtIndex(slotIndex)?.stack

    fun getItemsAtSlots(vararg slotIndexes: Int): List<ItemStack> {
        return slotIndexes.toList().mapNotNull(::getItemAtSlotIndex)
    }

    fun getSlotAtIndex(slotIndex: Int): Slot? = getItemsInOpenChest().find { it.slotIndex == slotIndex }

    fun NeuInternalName.getAmountInInventory(): Int = countItemsInLowerInventory { it.getInternalNameOrNull() == this }

    fun Slot.isTopInventory() = inventory.isTopInventory()

    fun IInventory.isTopInventory() = this is ContainerLocalMenu

    fun closeInventory() {
        Minecraft.getMinecraft().currentScreen = null
    }

    fun isInNormalChest(): Boolean = openInventoryName() in normalChestInternalNames.map { I18n.format(it) }
}
