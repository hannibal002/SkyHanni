package at.hannibal2.skyhanni.events.render.gui

import at.hannibal2.skyhanni.events.LorenzEvent
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack

class ReplaceItemEvent(val inventory: IInventory, val originalItem: ItemStack, val slot: Int) : LorenzEvent() {
    var replacement: ItemStack? = null
        private set

    fun replace(replacement: ItemStack?) {
        this.replacement = replacement
    }
}
