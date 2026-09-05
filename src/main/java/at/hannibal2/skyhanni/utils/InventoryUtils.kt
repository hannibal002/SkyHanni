package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.data.OtherInventoryData
import at.hannibal2.skyhanni.data.SackApi.getAmountInSacks
import at.hannibal2.skyhanni.events.MouseClickType
import at.hannibal2.skyhanni.utils.EntityUtils.getArmorInventory
import at.hannibal2.skyhanni.utils.InventoryUtils.clickSlot
import at.hannibal2.skyhanni.utils.InventoryUtils.mouseClickSlot
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.takeUnlessEmpty
import at.hannibal2.skyhanni.utils.collection.TimeLimitedSet
import at.hannibal2.skyhanni.utils.compat.InventoryCompat
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.convertEmptyToNull
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.filterNotNullOrEmpty
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.isNotEmpty
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.orNull
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.SkyHanniGuiContainer
import at.hannibal2.skyhanni.utils.compat.container
import at.hannibal2.skyhanni.utils.compat.normalizeAsArray
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.resources.language.I18n
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.Slot
import kotlin.time.Duration.Companion.seconds

// TODO refactor
@Suppress("MemberVisibilityCanBePrivate", "TooManyFunctions", "Unused")
object InventoryUtils {
    var itemInHandId = NeuInternalName.NONE
    fun NeuInternalName.recentlyHeld(): Boolean = this in recentItemsInHand

    val recentItemsInHand = TimeLimitedSet<NeuInternalName>(30.seconds)
    var latestItemInHand: SafeItemStack? = null
    val pastItemsInHand = mutableListOf<Pair<SimpleTimeMark, NeuInternalName>>()
    private val normalChestInternalNames = setOf("container.chest", "container.chestDouble")

    var lastItemChangeTime = SimpleTimeMark.farPast()

    fun getItemInHandAtTime(time: SimpleTimeMark): NeuInternalName? {
        return pastItemsInHand.lastOrNull { it.first <= time }?.second
    }

    fun getItemInHandDuringTimeframe(start: SimpleTimeMark, end: SimpleTimeMark): NeuInternalName? {
        val itemAtStart = getItemInHandAtTime(start)
        val itemAtEnd = getItemInHandAtTime(end)

        if (itemAtStart == null || itemAtEnd == null || itemAtStart != itemAtEnd) {
            return null
        }

        val changesBetween = pastItemsInHand.any {
            it.first > start && it.first <= end && it.second != itemAtStart
        }

        return if (changesBetween) null else itemAtStart
    }

    fun getItemsInOpenChest(): List<Slot> {
        return getItemsInOpenChestWithNull().filter { it.item.isNotEmpty() }
    }

    fun getItemsInOpenChestWithNull(): List<Slot> {
        val guiChest = MinecraftCompat.screen as? ContainerScreen ?: return emptyList()
        return guiChest.slots()
            .filter { it.container !is Inventory }
    }

    fun getItemIdsInOpenChest(): Set<NeuInternalName> {
        return getItemsInOpenChest().mapNotNull { it.item.getInternalNameOrNull() }.toSet()
    }

    /**
     * Only works while not in an inventory.
     */
    fun getSlotsInOwnInventory(): List<Slot> {
        val guiInventory = MinecraftCompat.screen as? SkyHanniGuiContainer ?: return emptyList()
        return guiInventory.slots()
            .filter { it.container is Inventory && it.item.isNotEmpty() }
    }

    fun openInventoryName(): String = OtherInventoryData.currentInventoryName

    fun inInventory() = MinecraftCompat.screen is ContainerScreen

    fun inOwnInventory() = MinecraftCompat.screen is InventoryScreen

    fun inAnyInventory() = inInventory() || inOwnInventory()

    fun inContainer() = MinecraftCompat.screen is SkyHanniGuiContainer

    fun getItemsInOwnInventory(): List<SafeItemStack> =
        getItemsInOwnInventoryWithNull()?.filterNotNullOrEmpty().orEmpty()

    fun getItemsInOwnInventoryWithNull(): Array<SafeItemStack?>? =
        MinecraftCompat.localPlayerOrNull?.inventory?.nonEquipmentItems?.normalizeAsArray().convertEmptyToNull()

    // TODO use this instead of getItemsInOwnInventory() for many cases, e.g. vermin tracker, diana spade, etc
    fun getItemsInHotbar(): List<SafeItemStack> =
        getItemsInOwnInventoryWithNull()?.slice(0..8)?.filterNotNull().orEmpty()

    fun containsInLowerInventory(predicate: (SafeItemStack) -> Boolean): Boolean =
        countItemsInLowerInventory(predicate) > 0

    fun containsInLowerInventoryInternalName(predicate: (NeuInternalName) -> Boolean): Boolean =
        countItemsInLowerInventoryInternalName(predicate) > 0

    fun countItemsInLowerInventory(predicate: (SafeItemStack) -> Boolean): Int =
        getItemsInOwnInventory().filter { predicate(it) }.sumOf { it.count }

    fun countItemsInLowerInventoryInternalName(predicate: (NeuInternalName) -> Boolean): Int =
        countItemsInLowerInventory { it.getInternalNameOrNull()?.let(predicate) ?: false }

    fun inStorage() = openInventoryName().let {
        (it.contains("Storage") && !it.contains("Rift Storage")) ||
            it.contains("Ender Chest") || it.contains("Backpack")
    }

    fun getItemInHand(): SafeItemStack? = MinecraftCompat.localPlayerOrNull?.mainHandItem?.takeUnlessEmpty()

    fun getArmor(): Array<SafeItemStack?> = MinecraftCompat.localPlayerOrNull?.getArmorInventory() ?: arrayOfNulls(4)
    fun getArmorInternalNames(): Set<NeuInternalName> = getArmor().mapNotNull { it?.getInternalNameOrNull() }.toSet()

    fun getHelmet(): SafeItemStack? = getArmor()[3]
    fun getChestplate(): SafeItemStack? = getArmor()[2]
    fun getLeggings(): SafeItemStack? = getArmor()[1]
    fun getBoots(): SafeItemStack? = getArmor()[0]

    fun isSlotInPlayerInventory(itemStack: SafeItemStack): Boolean {
        val slotUnderMouse = InventoryCompat.slotUnderCursor() ?: return false
        return slotUnderMouse.container is Inventory && slotUnderMouse.item == itemStack
    }

    fun isItemInInventory(name: NeuInternalName) = name.getAmountInInventory() > 0

    fun ChestMenu.getUpperItems(): Map<Slot, SafeItemStack> = buildMap {
        for ((slot, stack) in getAllItems()) {
            if (slot.index != slot.containerSlot) continue
            this[slot] = stack
        }
    }

    fun ChestMenu.getLowerItems(): Map<Slot, SafeItemStack> = buildMap {
        for ((slot, stack) in getAllItems()) {
            if (slot.index == slot.containerSlot) continue
            this[slot] = stack
        }
    }

    fun ChestMenu.getAllItems(): Map<Slot, SafeItemStack> = buildMap {
        for (slot in slots) {
            if (slot == null) continue
            val stack = slot.item.orNull() ?: continue
            this[slot] = stack
        }
    }

    fun ChestMenu.getAllSlots(): Map<Slot, SafeItemStack?> = buildMap {
        for (slot in slots) {
            if (slot == null) continue
            this[slot] = slot.item
        }
    }

    fun getItemAtSlotIndex(slotIndex: Int): SafeItemStack? = getSlotAtIndex(slotIndex)?.item

    fun getItemsAtSlots(vararg slotIndexes: Int): List<SafeItemStack> {
        return slotIndexes.toList().mapNotNull(::getItemAtSlotIndex)
    }

    fun getSlotAtIndex(slotIndex: Int): Slot? = getItemsInOpenChest().find { it.containerSlot == slotIndex }

    fun NeuInternalName.getAmountInInventory(): Int = countItemsInLowerInventoryInternalName { it == this }

    fun NeuInternalName.getAmountInInventoryAndSacks(): Int = getAmountInInventory() + getAmountInSacks()

    fun Slot.isTopInventory() = container.isTopInventory()

    fun Container.isTopInventory() = this is SimpleContainer

    fun closeInventory() {
        MinecraftCompat.screen = null
    }

    fun isInNormalChest(name: String = openInventoryName()): Boolean =
        name in normalChestInternalNames.map { I18n.get(it) }

    /**
     * Clicks a slot by calling MultiPlayerGameMode's `handleContainerInput` method.
     * Less compatible with other mods that intercept clicks than [mouseClickSlot].
     */
    fun clickSlot(
        slotId: Int,
        windowId: Int = InventoryCompat.getWindowId(),
        button: MouseClickType = LEFT_CLICK,
        mode: ContainerInput = button.defaultMode,
    ) {
        clickSlotRaw(slotId, windowId, button.buttonId, mode)
    }

    /**
     * Same as [clickSlot], but takes the window id from this screen.
     */
    fun ContainerScreen.clickSlot(
        slotId: Int,
        button: MouseClickType = LEFT_CLICK,
        mode: ContainerInput = button.defaultMode,
    ) {
        clickSlot(slotId, container.containerId, button, mode)
    }

    /**
     * Only for forwarding an existing click. What [button] means depends on [mode],
     * see [MouseClickType]. Prefer [clickSlot] everywhere else.
     */
    fun clickSlotRaw(
        slotId: Int,
        windowId: Int = InventoryCompat.getWindowId(),
        button: Int = 0,
        mode: ContainerInput = PICKUP,
    ) {
        InventoryCompat.clickInventorySlot(windowId, slotId, button, mode)
    }

    /**
     * Clicks a slot by calling the AbstractContainerScreen's `slotClicked` method.
     * Preferred over [clickSlot] for compatibility with other mods, especially when
     * modifying an existing player click.
     */
    fun mouseClickSlot(
        slotId: Int,
        button: MouseClickType = LEFT_CLICK,
        mode: ContainerInput = button.defaultMode,
    ) {
        mouseClickSlotRaw(slotId, button.buttonId, mode)
    }

    /**
     * Only for forwarding an existing click. What [button] means depends on [mode],
     * see [MouseClickType]. Prefer [mouseClickSlot] everywhere else.
     */
    fun mouseClickSlotRaw(
        slotId: Int,
        button: Int = 0,
        mode: ContainerInput = PICKUP,
    ) {
        InventoryCompat.mouseClickInventorySlot(slotId, button, mode)
    }

    fun SkyHanniGuiContainer.slots(): List<Slot> {
        return InventoryCompat.containerSlots(this)
    }
}
