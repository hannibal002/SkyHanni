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
    //? if < 26.1 {
    return ItemStack(item, count)
    //? } else {
    /*return DeferredItemStack(item, { ItemStack(item, count) }, count)*/
    //?}
}

/**
 * Safely creates an [ItemStack] from [item] with [count], then applies [extraOps].
 * Returns [ItemStack.EMPTY] if components are not yet loaded.
 */
fun SafeItemStack(item: Item, count: Int = 1, extraOps: SafeItemStack.() -> Unit): SafeItemStack {
    //? if < 26.1 {
    return ItemStack(item, count).also(extraOps)
    //? } else {
    /*return DeferredItemStack(item, { ItemStack(item, count).also(extraOps) }, count)*/
    //?}
}

//? if < 26.1 {
val SafeItemStack.itemType: Item get() = item
//? } else
//val SafeItemStack.itemType: Item get() = item.value()
