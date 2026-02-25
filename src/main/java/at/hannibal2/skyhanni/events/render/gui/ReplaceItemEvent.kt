package at.hannibal2.skyhanni.events.render.gui

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.isNotEmpty
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack

class ReplaceItemEvent(val inventory: Container, val originalItem: ItemStack, val slot: Int) : SkyHanniEvent() {
    var replacement: ItemStack? = null
        private set
    var shouldRemove = false
        private set

    val hasItem: Boolean = originalItem.isNotEmpty()

    fun replace(replacement: ItemStack) {
        this.replacement = replacement
    }

    fun remove() {
        shouldRemove = true
    }

    companion object {
        @JvmStatic
        fun postEvent(
            inventory: Container,
            originalItem: ItemStack,
            slot: Int,
        ): ItemStack {
            val event = ReplaceItemEvent(inventory, originalItem, slot)
            event.post()
            return if (event.shouldRemove) ItemStack.EMPTY
            else event.replacement ?: originalItem
        }
    }
}
