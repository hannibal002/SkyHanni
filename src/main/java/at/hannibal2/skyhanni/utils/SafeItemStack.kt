package at.hannibal2.skyhanni.utils

import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

/**
 * Crash-safe drop-in replacement for [ItemStack] in Minecraft 26.1+.
 *
 * Use [SafeItemStack] everywhere you previously used [ItemStack] as a type.
 * For construction, call `SafeItemStack(item)` instead of `ItemStack(item)` -
 * the top-level factory functions below guard against "Components not bound yet"
 * crashes by returning [ItemStack.EMPTY] when component data is not yet ready.
 *
 * Static access (`SafeItemStack.EMPTY`, etc.) works identically to [ItemStack].
 *
 * @see SafeItemStackUtils
 */
typealias SafeItemStack = ItemStack

/**
 * Safely creates an [ItemStack] from [item] with [count].
 * Returns [ItemStack.EMPTY] if components are not yet loaded.
 */
fun SafeItemStack(item: Item, count: Int = 1): SafeItemStack {
    //~ if > 1.21.11 'ItemStack(item, count)' -> 'DeferredItemStack(item, { ItemStack(item, count) }, count)'
    return ItemStack(item, count)
}

/**
 * Safely creates an [ItemStack] from [item] with [count], then applies [extraOps].
 * Returns [ItemStack.EMPTY] if components are not yet loaded.
 */
fun SafeItemStack(item: Item, count: Int = 1, extraOps: SafeItemStack.() -> Unit): SafeItemStack {
    //~ if > 1.21.11 'ItemStack(item, count).also(extraOps)' -> 'DeferredItemStack(item, { ItemStack(item, count).also(extraOps) }, count)'
    return ItemStack(item, count).also(extraOps)
}

//~ if > 1.21.11 '= item' -> '= item!!.value()'
val SafeItemStack.itemType: Item get() = item
