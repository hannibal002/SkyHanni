package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.renderables.ItemStackProvider
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

/**
 * An [ItemStackProvider] for vanilla Minecraft items that defers [ItemStack] creation
 * until the item's component data is fully bound, avoiding "Components not bound yet"
 * crashes on 26.1+. Returns [ItemStack.EMPTY] on any access before components are ready,
 * then builds and caches on the first safe access.
 *
 * Prefer [Item.asProvider] for the common case.
 */
class VanillaItemStackProvider(
    private val item: () -> Item,
    private val extraOps: (ItemStack.() -> Unit)? = null,
) : ItemStackProvider {
    private var cached: ItemStack? = null

    override val stack get() = cached?.copy() ?: buildStack()

    private fun buildStack(): ItemStack {
        val built = if (extraOps != null) SafeItemStack(item()) { extraOps.invoke(this) } else SafeItemStack(item())
        if (built.isEmpty) return ItemStack.EMPTY
        cached = built
        return built.copy()
    }
}

fun Item.asProvider(extraOps: (ItemStack.() -> Unit)? = null): ItemStackProvider =
    VanillaItemStackProvider({ this }, extraOps)
