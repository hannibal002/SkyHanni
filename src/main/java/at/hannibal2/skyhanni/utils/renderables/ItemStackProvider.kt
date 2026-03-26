package at.hannibal2.skyhanni.utils.renderables

import net.minecraft.world.item.ItemStack
import kotlin.reflect.KProperty

interface ItemStackProvider {
    val stack: ItemStack

    operator fun getValue(thisRef: Any?, property: KProperty<*>): ItemStack = stack
}

class ItemStackDirectProvider(override val stack: ItemStack) : ItemStackProvider {
    companion object {
        fun ItemStack.asProvider(): ItemStackProvider = ItemStackDirectProvider(this)
    }
}
