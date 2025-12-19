package at.hannibal2.skyhanni.events.render.gui

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

class ReplaceItemEvent(val inventory: Container, val originalItem: ItemStack?, val slot: Int) : SkyHanniEvent() {
    var replacement: ItemStack? = null
        private set
    var shouldRemove = false
        private set

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
            inventoryContents: Array<ItemStack?>,
            slot: Int,
            cir: CallbackInfoReturnable<ItemStack>,
        ) {
            val originalItem = inventoryContents.getOrNull(slot)
            val event = ReplaceItemEvent(inventory, originalItem, slot)
            event.post()
            if (event.shouldRemove) {
                cir.returnValue = ItemStack.EMPTY
                return
            }
            event.replacement?.let { cir.returnValue = it }
        }
    }
}
