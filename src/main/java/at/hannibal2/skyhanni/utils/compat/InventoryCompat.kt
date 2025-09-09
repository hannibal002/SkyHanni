package at.hannibal2.skyhanni.utils.compat

import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import at.hannibal2.skyhanni.utils.compat.SkyHanniGuiContainer
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.GenericContainerScreenHandler
import net.minecraft.screen.slot.Slot
import net.minecraft.item.ItemStack
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

//#if FABRIC
import net.minecraft.screen.slot.SlotActionType
import at.hannibal2.skyhanni.compat.ReiCompat
//#endif

fun ClientPlayerEntity.getItemOnCursor(): ItemStack? {
    //#if MC < 1.21
    //$$ return this.inventory?.cursorStack
    //#else
    val stack = this.currentScreenHandler?.cursorStack
    if (stack?.isEmpty == true) return null
    return stack
    //#endif
}

fun stackUnderCursor(): ItemStack? {
    val screen = MinecraftClient.getInstance().currentScreen as? SkyHanniGuiContainer ?: return null
    //#if FORGE
    //$$ return screen.slotUnderMouse?.item
    //#else
    var stack = screen.focusedSlot?.stack
    if (stack != null) return stack
    stack = ReiCompat.getHoveredStackFromRei()
    return stack
    //#endif
}

fun slotUnderCursor(): Slot? {
    val screen = MinecraftClient.getInstance().currentScreen as? SkyHanniGuiContainer ?: return null
    //#if FORGE
    //$$ return screen.slotUnderMouse
    //#else
    return screen.focusedSlot
    //#endif
}

val GenericContainerScreen.container: ScreenHandler
    //#if MC < 1.16
    //$$ get() = this.inventorySlots
//#else
get() = this.screenHandler
//#endif

object InventoryCompat {

    // TODO add cache that persists until the next gui/window open/close packet is sent/received
    fun getOpenChestName(): String {
        val currentScreen = MinecraftClient.getInstance().currentScreen
        //#if MC < 1.16
        //$$ if (currentScreen !is GuiChest) return ""
        //$$ val value = currentScreen.inventorySlots as ContainerChest
        //$$ return value.lowerChestInventory?.displayName?.unformattedText.orEmpty()
        //#else
        return currentScreen?.title.formattedTextCompat()
        //#endif
    }


    fun clickInventorySlot(slot: Int, windowId: Int? = getWindowId(), mouseButton: Int, mode: Int) {
        windowId ?: return
        val controller = MinecraftClient.getInstance().interactionManager ?: return
        val player = MinecraftClient.getInstance().player ?: return
        //#if FORGE
        //$$ controller.windowClick(windowId, slot, mouseButton, mode, player)
        //#else
        controller.clickSlot(windowId, slot, mouseButton, SlotActionType.entries[mode], player)
        //#endif
    }

    fun containerSlots(container: SkyHanniGuiContainer): List<Slot> =
        //#if FORGE
        //$$ container.container.slots
//#else
container.screenHandler.slots
//#endif

    private fun getWindowId(): Int? =
        //#if FORGE
        //$$ (Minecraft.getInstance().screen as? ContainerScreen)?.container?.containerId
//#else
(MinecraftClient.getInstance().currentScreen as? GenericContainerScreen)?.screenHandler?.syncId
//#endif

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
