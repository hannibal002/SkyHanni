package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.InventoryData
import net.minecraft.item.ItemStack

class InventoryOpenEvent(val inventory: InventoryData.Inventory): LorenzEvent() {

    val inventoryId: Int by lazy { inventory.windowId }
    val inventoryName: String by lazy {inventory.title }
    val inventorySize: Int by lazy {inventory.slotCount }
    val inventoryItems: MutableMap<Int, ItemStack>  by lazy {inventory.items }
}