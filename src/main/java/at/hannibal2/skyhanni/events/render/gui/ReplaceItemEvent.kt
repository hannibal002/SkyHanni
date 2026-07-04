package at.hannibal2.skyhanni.events.render.gui

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.isNotEmpty
import net.minecraft.world.Container

/**
 * Fired when an item is read from a `SimpleContainer` slot, allowing handlers to replace
 * or remove the displayed item without modifying the underlying inventory.
 *
 * Fired on the main client thread via a Mixin into `SimpleContainer.getItem`.
 *
 * Call [replace] to substitute a different item, or [remove] to hide the slot entirely.
 * If multiple handlers call [replace], the last one wins.
 * [hasItem] is false when the original slot is empty.
 *
 * @param inventory the container whose slot is being accessed
 * @param originalItem the item currently in the slot
 * @param slot the slot index being accessed
 */
class ReplaceItemEvent(val inventory: Container, val originalItem: SafeItemStack, val slot: Int) : SkyHanniEvent() {
    var replacement: SafeItemStack? = null
        private set
    var shouldRemove = false
        private set

    val hasItem: Boolean = originalItem.isNotEmpty()

    fun replace(replacement: SafeItemStack) {
        this.replacement = replacement
    }

    fun remove() {
        shouldRemove = true
    }

    companion object {
        private val postDepth = ThreadLocal.withInitial { 0 }

        @JvmStatic
        fun postEvent(
            inventory: Container,
            originalItem: SafeItemStack,
            slot: Int,
        ): SafeItemStack {
            if (postDepth.get() > 0) return originalItem

            postDepth.set(postDepth.get() + 1)
            try {
                val event = ReplaceItemEvent(inventory, originalItem, slot)
                event.post()
                return if (event.shouldRemove) SafeItemStack.EMPTY
                else event.replacement ?: originalItem
            } finally {
                val depth = postDepth.get() - 1
                if (depth == 0) postDepth.remove()
                else postDepth.set(depth)
            }
        }
    }
}
