package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.mixins.transformers.gui.AccessorHandledScreen
import at.hannibal2.skyhanni.test.command.ErrorManager
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import at.hannibal2.skyhanni.utils.compat.SkyHanniGuiContainer
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
//#if FABRIC
import net.minecraft.world.inventory.ClickType
import at.hannibal2.skyhanni.compat.ReiCompat
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
//#endif

fun LocalPlayer.getItemOnCursor(): ItemStack? {
    //#if MC < 1.21
    //$$ return this.inventory?.cursorStack
    //#else
    val stack = this.containerMenu?.carried
    if (stack?.isEmpty == true) return null
    return stack
    //#endif
}

fun stackUnderCursor(): ItemStack? {
    val screen = Minecraft.getInstance().screen as? SkyHanniGuiContainer ?: return null
    //#if FORGE
    //$$ return screen.slotUnderMouse?.item
    //#else
    var stack = screen.hoveredSlot?.item
    if (stack != null) return stack
    stack = ReiCompat.getHoveredStackFromRei()
    return stack
    //#endif
}

fun slotUnderCursor(): Slot? {
    val screen = Minecraft.getInstance().screen as? SkyHanniGuiContainer ?: return null
    //#if FORGE
    //$$ return screen.slotUnderMouse
    //#else
    return screen.hoveredSlot
    //#endif
}

val ContainerScreen.container: AbstractContainerMenu
    //#if MC < 1.16
    //$$ get() = this.inventorySlots
//#else
get() = this.menu
//#endif

object InventoryCompat {

    /**
     * Internal method, not meant to be called directly. Prefer `InventoryUtils.clickSlot()`.
     */
    fun clickInventorySlot(windowId: Int, slotId: Int, mouseButton: Int, mode: Int) {
        val controller = Minecraft.getInstance().gameMode ?: return
        val player = Minecraft.getInstance().player ?: return
        //#if FORGE
        //$$ controller.windowClick(windowId, slotId, mouseButton, mode, player)
        //#else
        controller.handleInventoryMouseClick(windowId, slotId, mouseButton, ClickType.entries[mode], player)
        //#endif
    }

    /**
     * Internal method, not meant to be called directly. Prefer `InventoryUtils.mouseClickSlot()`.
     */
    fun mouseClickInventorySlot(slot: Int, mouseButton: Int, mode: Int) {
        if (slot < 0) return
        val gui = Minecraft.getInstance().screen
        //#if FORGE
        //$$ if (gui is SkyHanniGuiContainer) {
        //$$     val accessor = gui as AccessorGuiContainer
        //$$     val slotObj = gui.container.getSlot(slot)
        //$$     accessor.handleMouseClick_skyhanni(slotObj, slot, mouseButton, mode)
        //$$ }
        //#else
        if (gui is AbstractContainerScreen<*>) {
            val accessor = gui as AccessorHandledScreen
            val slotObj = gui.menu.getSlot(slot)
            val actionType = ClickType.entries[mode]
            accessor.handleMouseClick_skyhanni(slotObj, slot, mouseButton, actionType)
        }
        //#endif
    }

    fun containerSlots(container: SkyHanniGuiContainer): List<Slot> =
        //#if FORGE
        //$$ container.container.slots
//#else
container.menu.slots
//#endif

    fun getWindowIdOrNull(): Int? =
        //#if FORGE
        //$$ (Minecraft.getInstance().screen as? ContainerScreen)?.container?.containerId
//#else
(Minecraft.getInstance().screen as? ContainerScreen)?.menu?.containerId
//#endif

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
        //#if MC > 1.21
        return !this.isEmpty
        //#else
        //$$ return true
        //#endif
    }

    fun ItemStack?.orNull(): ItemStack? {
        //#if MC > 1.21
        return this?.takeUnless { it.isEmpty }
        //#endif
        return this
    }
}
