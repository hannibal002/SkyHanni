package at.hannibal2.skyhanni.utils.compat

import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
//#if FABRIC
//$$ import net.minecraft.screen.slot.SlotActionType
//#endif

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

fun GuiContainer.containerSlots(): List<Slot> =
    //#if FORGE
    inventorySlots.inventorySlots
//#else
//$$ screenHandler.slots
//#endif

private fun getWindowId(): Int? =
    //#if FORGE
    (Minecraft.getMinecraft().currentScreen as? GuiChest)?.inventorySlots?.windowId
//#else
//$$ (MinecraftClient.getInstance().currentScreen as? GenericContainerScreen)?.screenHandler?.syncId
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
