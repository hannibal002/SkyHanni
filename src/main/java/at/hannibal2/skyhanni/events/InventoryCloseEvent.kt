package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.OtherInventoryData
import net.minecraft.item.ItemStack

class InventoryCloseEvent(val inventory: OtherInventoryData.Inventory, val reopenSameName: Boolean) : LorenzEvent() {
    val inventoryId: Int by lazy { inventory.windowId }
    val inventoryName: String by lazy { inventory.title }
    val inventorySize: Int by lazy { inventory.slotCount }
    val inventoryItems: Map<Int, ItemStack> by lazy { inventory.items }
}
