package at.hannibal2.skyhanni.events

import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack

class ReplaceItemEvent(val original: ItemStack, val inventory: IInventory, val slotNumber: Int) : LorenzEvent() {
    var replacement: ItemStack?

    init {
        replacement = original
    }

    fun replaceWith(`is`: ItemStack?) {
        replacement = `is`
    }
}

