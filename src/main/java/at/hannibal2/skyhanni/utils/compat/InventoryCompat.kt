package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.compat.ReiCompat
import at.hannibal2.skyhanni.mixins.transformers.gui.AccessorHandledScreen
import at.hannibal2.skyhanni.test.command.ErrorManager
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

fun LocalPlayer.getItemOnCursor(): ItemStack? {
    val stack = this.containerMenu?.carried
    if (stack?.isEmpty == true) return null
    return stack
}

fun stackUnderCursor(): ItemStack? {
    val screen = Minecraft.getInstance().screen as? SkyHanniGuiContainer ?: return null
    var stack = screen.hoveredSlot?.item
    if (stack != null) return stack
    stack = ReiCompat.getHoveredStackFromRei()
    return stack
}

fun slotUnderCursor(): Slot? {
    val screen = Minecraft.getInstance().screen as? SkyHanniGuiContainer ?: return null
    return screen.hoveredSlot
}

val ContainerScreen.container: AbstractContainerMenu
    get() = this.menu

object InventoryCompat {

    /**
     * Internal method, not meant to be called directly. Prefer `InventoryUtils.clickSlot()`.
     */
    fun clickInventorySlot(windowId: Int, slotId: Int, mouseButton: Int, mode: Int) {
        val controller = Minecraft.getInstance().gameMode ?: return
        val player = Minecraft.getInstance().player ?: return
        controller.handleInventoryMouseClick(windowId, slotId, mouseButton, ClickType.entries[mode], player)
    }

    /**
     * Internal method, not meant to be called directly. Prefer `InventoryUtils.mouseClickSlot()`.
     */
    fun mouseClickInventorySlot(slot: Int, mouseButton: Int, mode: Int) {
        if (slot < 0) return
        val gui = Minecraft.getInstance().screen
        if (gui is AbstractContainerScreen<*>) {
            val accessor = gui as AccessorHandledScreen
            val slotObj = gui.menu.getSlot(slot)
            val actionType = ClickType.entries[mode]
            accessor.handleMouseClick_skyhanni(slotObj, slot, mouseButton, actionType)
        }
    }

    fun containerSlots(container: SkyHanniGuiContainer): List<Slot> =
        container.menu.slots

    fun getWindowIdOrNull(): Int? =
        (Minecraft.getInstance().screen as? ContainerScreen)?.menu?.containerId

    fun getWindowId(): Int =
        getWindowIdOrNull() ?: ErrorManager.skyHanniError("windowId is null")

    fun Array<ItemStack?>?.filterNotNullOrEmpty(): List<ItemStack>? {
        return this?.filterNotNull()?.filter { it.isNotEmpty() }
    }

    fun Array<ItemStack?>?.convertEmptyToNull(): Array<ItemStack?>? {
        if (this == null) return null
        if (this.isEmpty()) return this
        val new: MutableList<ItemStack?> = mutableListOf()
        for (stack in this) {
            if (!stack.isNotEmpty()) new.add(null)
            else new.add(stack)
        }
        return new.normalizeAsArray()
    }

    @OptIn(ExperimentalContracts::class)
    fun ItemStack?.isNotEmpty(): Boolean {
        contract {
            returns(true) implies (this@isNotEmpty != null)
        }
        this ?: return false
        return !this.isEmpty
    }

    fun ItemStack?.orNull(): ItemStack? {
        return this?.takeUnless { it.isEmpty }
    }
}
