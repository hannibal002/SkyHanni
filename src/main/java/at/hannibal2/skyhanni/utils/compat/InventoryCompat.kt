package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.utils.InventoryUtils.getWindowId
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.item.ItemStack

fun clickInventorySlot(slot: Int, windowId: Int? = getWindowId(), mouseButton: Int = 0, mode: Int = 0) {
    windowId ?: return
    val controller = Minecraft.getMinecraft().playerController ?: return
    //#if MC < 1.12
    controller.windowClick(windowId, slot, mouseButton, mode, Minecraft.getMinecraft().thePlayer)
    //#else
    //$$ controller.windowClick(windowId, slot, mouseButton, ClickType.entries[mode], Minecraft.getMinecraft().player)
    //#endif
}

fun EntityPlayerSP.getItemOnCursor(): ItemStack? {
    //#if MC < 1.21
    return this.inventory?.itemStack
    //#else
    //$$ val stack = this.currentScreenHandler?.cursorStack
    //$$ if (stack?.isEmpty == true) return null
    //$$ return stack
    //#endif
}

