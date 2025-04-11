package at.hannibal2.skyhanni.utils.compat

import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.inventory.Container
import net.minecraft.inventory.ContainerChest
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack

//#if FABRIC
//$$ import net.minecraft.screen.slot.SlotActionType
//#endif

fun EntityPlayerSP.getItemOnCursor(): ItemStack? {
    //#if MC < 1.21
    return this.inventory?.itemStack
    //#else
    //$$ val stack = this.currentScreenHandler?.cursorStack
    //$$ if (stack?.isEmpty == true) return null
    //$$ return stack
    //#endif
}

fun slotUnderCursor(): Slot? {
    val screen = Minecraft.getMinecraft().currentScreen as? GuiContainer ?: return null
    //#if FORGE
    return screen.slotUnderMouse
    //#else
    //$$ return screen.getSlotUnderMouse()
    //#endif
}

val GuiChest.container: Container
    //#if MC < 1.16
    get() = this.inventorySlots
//#else
//$$ get() = this.screenHandler
//#endif

object InventoryCompat {

    // TODO add cache that persists until the next gui/window open/close packet is sent/received
    fun getOpenChestName(): String {
        val currentScreen = Minecraft.getMinecraft().currentScreen
        //#if MC < 1.16
        if (currentScreen !is GuiChest) return ""
        val value = currentScreen.inventorySlots as ContainerChest
        return value.lowerChestInventory?.displayName?.unformattedText.orEmpty()
        //#else
        //$$ return currentScreen?.title.formattedTextCompat()
        //#endif
    }


    fun clickInventorySlot(slot: Int, windowId: Int? = getWindowId(), mouseButton: Int = 0, mode: Int = 0) {
        windowId ?: return
        val controller = Minecraft.getMinecraft().playerController ?: return
        val player = Minecraft.getMinecraft().thePlayer ?: return
        //#if FORGE
        controller.windowClick(windowId, slot, mouseButton, mode, player)
        //#else
        //$$ controller.clickSlot(windowId, slot, mouseButton, SlotActionType.entries[mode], player)
        //#endif
    }

    fun containerSlots(container: GuiContainer): List<Slot> =
        //#if FORGE
        container.inventorySlots.inventorySlots
//#else
//$$ container.screenHandler.slots
//#endif

    private fun getWindowId(): Int? =
        //#if FORGE
        (Minecraft.getMinecraft().currentScreen as? GuiChest)?.inventorySlots?.windowId
//#else
//$$ (MinecraftClient.getInstance().currentScreen as? GenericContainerScreen)?.screenHandler?.syncId
//#endif

}
