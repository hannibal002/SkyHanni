package at.hannibal2.skyhanni.utils

import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

/**
 * Crash-safe replacement for `ItemStack(item)` construction in Minecraft 26.1+.
 *
 * In 26.1, calling `ItemStack(item)` before item component data is bound throws
 * "Components not bound yet". Use `SafeItemStack(item)` instead — the `invoke`
 * operator guards creation behind a [componentsLoaded] check, returning
 * [ItemStack.EMPTY] when components are not yet ready.
 *
 * Since [ItemStack] is final, [SafeItemStack] is an object with an `invoke` operator
 * that mirrors the [ItemStack] constructor signatures. The result is still a plain
 * [ItemStack], so it is usable anywhere [ItemStack] is accepted.
 *
 * @see SafeItemStackUtils
 */
object SafeItemStack {

    private val componentsLoaded get() = SafeItemStackUtils.componentsLoaded

    /**
     * Creates an [ItemStack] from [item] with [count].
     * Returns [ItemStack.EMPTY] if components are not yet loaded.
     */
    operator fun invoke(item: Item, count: Int = 1): ItemStack {
        if (!componentsLoaded) return ItemStack.EMPTY
        return ItemStack(item, count)
    }

    /**
     * Creates an [ItemStack] from [item] with [count], then applies [extraOps].
     * Returns [ItemStack.EMPTY] if components are not yet loaded.
     */
    operator fun invoke(item: Item, count: Int = 1, extraOps: ItemStack.() -> Unit): ItemStack {
        if (!componentsLoaded) return ItemStack.EMPTY
        return ItemStack(item, count).also(extraOps)
    }
}
