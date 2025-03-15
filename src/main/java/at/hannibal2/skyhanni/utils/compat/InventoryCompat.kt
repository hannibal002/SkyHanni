package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.utils.InventoryUtils.getWindowId
import net.minecraft.client.Minecraft

fun clickInventorySlot(slot: Int, windowId: Int? = getWindowId(), mouseButton: Int = 0, mode: Int = 0) {
    windowId ?: return
    val controller = Minecraft.getMinecraft().playerController ?: return
    //#if MC < 1.12
    controller.windowClick(windowId, slot, mouseButton, mode, Minecraft.getMinecraft().thePlayer)
    //#else
    //$$ controller.windowClick(windowId, slot, mouseButton, ClickType.entries[mode], Minecraft.getMinecraft().player)
    //#endif
}


