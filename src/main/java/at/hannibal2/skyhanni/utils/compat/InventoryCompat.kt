package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.InventoryUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.Slot
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

// TODO 26.1 REI compat needed
//? if < 26.1
//import at.hannibal2.skyhanni.compat.ReiCompat

fun LocalPlayer.getItemOnCursor(): SafeItemStack? {
    val stack = this.containerMenu.carried
    if (stack.isEmpty) return null
    return stack
}

fun stackUnderCursor(): SafeItemStack? {
    val screen = Minecraft.getInstance().screen as? SkyHanniGuiContainer ?: return null
    var stack = screen.hoveredSlot?.item
    if (stack != null) return stack
    // TODO 26.1 REI compat needed
    //? if < 26.1
    //stack = ReiCompat.getHoveredStackFromRei()
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
     * Internal method, not meant to be called directly. Prefer [InventoryUtils.clickSlot].
     */
    internal fun clickInventorySlot(windowId: Int, slotId: Int, mouseButton: Int, mode: ContainerInput) {
        val controller = Minecraft.getInstance().gameMode ?: return
        val player = Minecraft.getInstance().player ?: return
        //~ if < 26.1 'handleContainerInput' -> 'handleInventoryMouseClick'
        controller.handleContainerInput(windowId, slotId, mouseButton, mode, player)
    }

    /**
     * Internal method, not meant to be called directly. Prefer [InventoryUtils.mouseClickSlot].
     */
    internal fun mouseClickInventorySlot(slot: Int, mouseButton: Int, mode: ContainerInput) {
        if (slot < 0) return
        val gui = Minecraft.getInstance().screen
        if (gui is AbstractContainerScreen<*>) {
            val slotObj = gui.menu.getSlot(slot)
            gui.slotClicked(slotObj, slot, mouseButton, mode)
        }
    }

    fun containerSlots(container: SkyHanniGuiContainer): List<Slot> =
        container.menu.slots

    fun getWindowIdOrNull(): Int? =
        (Minecraft.getInstance().screen as? ContainerScreen)?.menu?.containerId

    fun getWindowId(): Int =
        getWindowIdOrNull() ?: ErrorManager.skyHanniError("windowId is null")

    fun Array<SafeItemStack?>?.filterNotNullOrEmpty(): List<SafeItemStack>? {
        return this?.filterNotNull()?.filter { it.isNotEmpty() }
    }

    fun Array<SafeItemStack?>?.convertEmptyToNull(): Array<SafeItemStack?>? {
        if (this == null) return null
        if (this.isEmpty()) return this
        val new: MutableList<SafeItemStack?> = mutableListOf()
        for (stack in this) {
            if (!stack.isNotEmpty()) new.add(null)
            else new.add(stack)
        }
        return new.normalizeAsArray()
    }

    @OptIn(ExperimentalContracts::class)
    fun SafeItemStack?.isNotEmpty(): Boolean {
        contract {
            returns(true) implies (this@isNotEmpty != null)
        }
        this ?: return false
        return !this.isEmpty
    }

    fun SafeItemStack?.orNull(): SafeItemStack? {
        return this?.takeUnless { it.isEmpty }
    }
}
