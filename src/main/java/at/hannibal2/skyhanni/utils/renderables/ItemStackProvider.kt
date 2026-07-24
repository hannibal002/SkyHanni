package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.utils.SafeItemStack
import kotlin.reflect.KProperty

interface ItemStackProvider {
    val stack: SafeItemStack

    operator fun getValue(thisRef: Any?, property: KProperty<*>): SafeItemStack = stack
}

class ItemStackDirectProvider(override val stack: SafeItemStack) : ItemStackProvider {
    companion object {
        fun SafeItemStack.asProvider(): ItemStackProvider = ItemStackDirectProvider(this)
    }
}
