package at.hannibal2.skyhanni.utils

import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

/**
 * Crash-safe drop-in replacement for [ItemStack] in Minecraft 26.1+.
 *
 * Use [SafeItemStack] everywhere you previously used [ItemStack] as a type.
 * For construction, call `SafeItemStack(item)` instead of `ItemStack(item)` —
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
@Suppress("FunctionName")
fun SafeItemStack(item: Item, count: Int = 1): SafeItemStack {
    if (!SafeItemStackUtils.componentsLoaded) return ItemStack.EMPTY
    return ItemStack(item, count)
}

/**
 * Safely creates an [ItemStack] from [item] with [count], then applies [extraOps].
 * Returns [ItemStack.EMPTY] if components are not yet loaded.
 */
@Suppress("FunctionName")
fun SafeItemStack(item: Item, count: Int = 1, extraOps: ItemStack.() -> Unit): SafeItemStack {
    if (!SafeItemStackUtils.componentsLoaded) return ItemStack.EMPTY
    return ItemStack(item, count).also(extraOps)
}
