@file:Suppress("VanillaItemStackImport")

package at.hannibal2.skyhanni.utils

import net.minecraft.world.item.Item

//? if >= 26.1
import net.minecraft.core.registries.BuiltInRegistries

/**
 * Central compatibility helpers for Minecraft item component binding.
 *
 * Minecraft 26.1 can expose item registry holders before their default components are bound.
 * Callers that need component reads should go through these helpers instead of checking
 * version-specific registry state locally.
 */
object SafeItemStackUtils {

    fun canBindComponents(item: Item?): Boolean {
        item ?: return false
        //~ if < 26.1 'return BuiltInRegistries.ITEM.wrapAsHolder(item).areComponentsBound()' -> 'return true'
        return BuiltInRegistries.ITEM.wrapAsHolder(item).areComponentsBound()
    }

    fun canReadComponents(stack: SafeItemStack): Boolean = canBindComponents(stack.itemType)
}

fun SafeItemStack.ensureComponentsBound(): SafeItemStack {
    //? if >= 26.1
    (this as? DeferredItemStack)?.bindComponentsIfReady()
    return this
}
