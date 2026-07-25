@file:Suppress("VanillaItemStackImport", "UnsafeCAllOnNullableType")

package at.hannibal2.skyhanni.utils

import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

//? if >= 26.1
import net.minecraft.world.item.ItemStackTemplate

/**
 * Crash-safe drop-in replacement for [ItemStack] in Minecraft 26.1+.
 *
 * Use [SafeItemStack] everywhere you previously used [ItemStack] as a type.
 * For construction, call `SafeItemStack(item)` instead of `ItemStack(item)` -
 * the top-level factory functions below guard against "Components not bound yet"
 * crashes by deferring stack creation when component data is not yet ready.
 *
 * Static access (`SafeItemStack.EMPTY`, etc.) works identically to [ItemStack].
 *
 * @see SafeItemStackUtils
 */
typealias SafeItemStack = ItemStack

/**
 * Safely creates an [ItemStack] from [item] with [count].
 * Returns [ItemStack.EMPTY] for empty item inputs.
 */
fun SafeItemStack(item: Item, count: Int = 1): SafeItemStack {
    if (count <= 0 || item == Items.AIR) return ItemStack.EMPTY
    //~ if < 26.1 'DeferredItemStack(item, { ItemStackTemplate(item, count) }, count)' -> 'ItemStack(item, count)'
    return DeferredItemStack(item, { ItemStackTemplate(item, count) }, count)
}

/**
 * Safely creates an [ItemStack] from [item] with [count], then applies [extraOps].
 * Returns [ItemStack.EMPTY] for empty item inputs.
 */
fun SafeItemStack(item: Item, count: Int = 1, extraOps: SafeItemStack.() -> Unit): SafeItemStack {
    if (count <= 0 || item == Items.AIR) return ItemStack.EMPTY
    //~ if < 26.1 'DeferredItemStack(item, { createItemStackTemplate(item, count, extraOps) }, count)' -> 'ItemStack(item, count).also(extraOps)'
    return DeferredItemStack(item, { createItemStackTemplate(item, count, extraOps) }, count)
}

//? if >= 26.1 {
private fun createItemStackTemplate(item: Item, count: Int, extraOps: SafeItemStack.() -> Unit): ItemStackTemplate =
    ItemStackTemplate.fromNonEmptyStack(ItemStackTemplate(item, count).create().also(extraOps))
//?}

//~ if < 26.1 ' item?.value() ?: Items.AIR' -> ' item'
val SafeItemStack.itemType: Item get() = item?.value() ?: Items.AIR
