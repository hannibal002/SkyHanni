package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.data.OtherInventoryData
import at.hannibal2.skyhanni.data.SackApi.getAmountInSacks
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent.ClickType
import at.hannibal2.skyhanni.utils.EntityUtils.getArmorInventory
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.collection.TimeLimitedSet
import at.hannibal2.skyhanni.utils.compat.InventoryCompat
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.convertEmptyToNull
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.filterNotNullOrEmpty
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.isNotEmpty
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.orNull
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.SkyHanniGuiContainer
import at.hannibal2.skyhanni.utils.compat.normalizeAsArray
import at.hannibal2.skyhanni.utils.compat.slotUnderCursor
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.resources.language.I18n
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import kotlin.time.Duration.Companion.seconds

@Suppress("TooManyFunctions", "Unused", "MemberVisibilityCanBePrivate")
object InventoryUtils {

    var itemInHandId = NeuInternalName.NONE
    fun NeuInternalName.recentlyHeld(): Boolean = this in recentItemsInHand

    val recentItemsInHand = TimeLimitedSet<NeuInternalName>(30.seconds)
    var latestItemInHand: ItemStack? = null
    val pastItemsInHand = mutableListOf<Pair<SimpleTimeMark, NeuInternalName>>()
    private val normalChestInternalNames = setOf("container.chest", "container.chestDouble")

    fun getItemInHandAtTime(time: SimpleTimeMark): NeuInternalName? {
        return pastItemsInHand.lastOrNull { it.first <= time }?.second
    }

    fun getItemsInOpenChest(): List<Slot> {
        return getItemsInOpenChestWithNull().filter { it.item.isNotEmpty() }
    }

    fun getItemsInOpenChestWithNull(): List<Slot> {
        val guiChest = Minecraft.getInstance().screen as? ContainerScreen ?: return emptyList()
        return guiChest.slots()
            .filter { it.container !is Inventory }
    }

    fun getItemIdsInOpenChest(): Set<NeuInternalName> {
        return getItemsInOpenChest().mapNotNull { it.item?.getInternalNameOrNull() }.toSet()
    }

    // only works while not in an inventory
    fun getSlotsInOwnInventory(): List<Slot> {
        val guiInventory = Minecraft.getInstance().screen as? SkyHanniGuiContainer ?: return emptyList()
        return guiInventory.slots()
            .filter { it.container is Inventory && it.item.isNotEmpty() }
    }

    fun openInventoryName(): String = OtherInventoryData.currentInventoryName

    fun inInventory() = Minecraft.getInstance().screen is ContainerScreen

    fun inOwnInventory() = Minecraft.getInstance().screen is InventoryScreen

    fun inAnyInventory() = inInventory() || inOwnInventory()

    fun inContainer() = Minecraft.getInstance().screen is SkyHanniGuiContainer

    fun getItemsInOwnInventory(): List<ItemStack> =
        getItemsInOwnInventoryWithNull()?.filterNotNullOrEmpty().orEmpty()

    fun getItemsInOwnInventoryWithNull(): Array<ItemStack?>? =
        MinecraftCompat.localPlayerOrNull?.inventory?.nonEquipmentItems?.normalizeAsArray().convertEmptyToNull()

    // TODO use this instead of getItemsInOwnInventory() for many cases, e.g. vermin tracker, diana spade, etc
    fun getItemsInHotbar(): List<ItemStack> =
        getItemsInOwnInventoryWithNull()?.slice(0..8)?.filterNotNull().orEmpty()

    fun containsInLowerInventory(predicate: (ItemStack) -> Boolean): Boolean =
        countItemsInLowerInventory(predicate) > 0

    fun countItemsInLowerInventory(predicate: (ItemStack) -> Boolean): Int =
        getItemsInOwnInventory().filter { predicate(it) }.sumOf { it.count }

    fun inStorage() = openInventoryName().let {
        (it.contains("Storage") && !it.contains("Rift Storage")) ||
            it.contains("Ender Chest") || it.contains("Backpack")
    }

    fun getItemInHand(): ItemStack? = MinecraftCompat.localPlayerOrNull?.mainHandItem

    fun getArmor(): Array<ItemStack?> = MinecraftCompat.localPlayerOrNull?.getArmorInventory() ?: arrayOfNulls(4)

    fun getHelmet(): ItemStack? = getArmor()[3]
    fun getChestplate(): ItemStack? = getArmor()[2]
    fun getLeggings(): ItemStack? = getArmor()[1]
    fun getBoots(): ItemStack? = getArmor()[0]

    fun GuiContainerEvent.SlotClickEvent.makeShiftClick() {
        if (this.clickedButton == 1 && slot?.item?.getItemCategoryOrNull() == ItemCategory.SACK) return
        slot?.index?.let { slotNumber ->
            clickSlot(slotNumber, container.containerId, mouseButton = 0, mode = ClickType.SHIFT)
            this.cancel()
        }
    }

    fun isSlotInPlayerInventory(itemStack: ItemStack): Boolean {
        val slotUnderMouse = slotUnderCursor() ?: return false
        return slotUnderMouse.container is Inventory && slotUnderMouse.item == itemStack
    }

    fun isItemInInventory(name: NeuInternalName) = name.getAmountInInventory() > 0

    fun ChestMenu.getUpperItems(): Map<Slot, ItemStack> = buildMap {
        for ((slot, stack) in getAllItems()) {
            if (slot.index != slot.containerSlot) continue
            this[slot] = stack
        }
    }

    fun ChestMenu.getLowerItems(): Map<Slot, ItemStack> = buildMap {
        for ((slot, stack) in getAllItems()) {
            if (slot.index == slot.containerSlot) continue
            this[slot] = stack
        }
    }

    fun ChestMenu.getAllItems(): Map<Slot, ItemStack> = buildMap {
        for (slot in slots) {
            if (slot == null) continue
            val stack = slot.item.orNull() ?: continue
            this[slot] = stack
        }
    }

    fun ChestMenu.getAllSlots(): Map<Slot, ItemStack?> = buildMap {
        for (slot in slots) {
            if (slot == null) continue
            this[slot] = slot.item
        }
    }

    fun getItemAtSlotIndex(slotIndex: Int): ItemStack? = getSlotAtIndex(slotIndex)?.item

    fun getItemsAtSlots(vararg slotIndexes: Int): List<ItemStack> {
        return slotIndexes.toList().mapNotNull(::getItemAtSlotIndex)
    }

    fun getSlotAtIndex(slotIndex: Int): Slot? = getItemsInOpenChest().find { it.containerSlot == slotIndex }

    fun NeuInternalName.getAmountInInventory(): Int = countItemsInLowerInventory { it.getInternalNameOrNull() == this }

    fun NeuInternalName.getAmountInInventoryAndSacks(): Int = getAmountInInventory() + getAmountInSacks()

    fun Slot.isTopInventory() = container.isTopInventory()

    fun Container.isTopInventory() = this is SimpleContainer

    fun closeInventory() {
        Minecraft.getInstance().screen = null
    }

    fun isInNormalChest(name: String = openInventoryName()): Boolean = name in normalChestInternalNames.map { I18n.get(it) }

    fun clickSlot(
        slotId: Int,
        windowId: Int = InventoryCompat.getWindowId(),
        mouseButton: Int = 0,
        mode: ClickType = ClickType.NORMAL,
    ) {
        InventoryCompat.clickInventorySlot(windowId, slotId, mouseButton, mode.id)
    }

    fun mouseClickSlot(
        slotId: Int,
        mouseButton: Int = 0,
        mode: ClickType = ClickType.NORMAL,
    ) {
        InventoryCompat.mouseClickInventorySlot(slotId, mouseButton, mode.id)
    }

    fun SkyHanniGuiContainer.slots(): List<Slot> {
        return InventoryCompat.containerSlots(this)
    }
}
