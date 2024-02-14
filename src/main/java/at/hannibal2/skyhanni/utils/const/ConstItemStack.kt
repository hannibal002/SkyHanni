package at.hannibal2.skyhanni.utils.const

import net.minecraft.item.EnumDyeColor
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTBase
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagString

/**
 * Access a shallow copy of the underlying item stack. Callers of this method promise not to modify the nbt data of
 * the returned instance, but can modify the stack size, metadata, item or outright replace the root tag compound.
 * Interior immutability of [ItemStack.stackTagCompound] needs to be guaranteed by the caller. The instance is owned
 * ergo not shared with anyone else, so that there are no mutability concerns about the ItemStack instance itself.
 */
fun Const<ItemStack>.getOwnedShallowCopy(): ItemStack {
    return ItemStack(unsafeMutable.item, unsafeMutable.stackSize, unsafeMutable.metadata, unsafeMutable.tagCompound)
}

/**
 * Access a copy of the underlying item stack. The returned copy is fully and deeply owned, so changes can be made
 * to the item stack instance, including interior mutability concerning the [ItemStack.stackTagCompound].
 */
fun Const<ItemStack>.getOwnedDeepCopy(): ItemStack {
    return unsafeMutable.copy()
}

/**
 * Returns the [ItemStack.stackSize]
 */
inline val Const<ItemStack>.stackSize: Int get() = unsafeMap(ItemStack::stackSize).unconst

/**
 * Returns the [item type](ItemStack.item)
 */
inline val Const<ItemStack>.itemType: Item get() = unsafeMap(ItemStack::getItem).unconst

/**
 * Returns the [damage or metadata](ItemStack.metadata)
 */
inline val Const<ItemStack>.damage: Int get() = unsafeMap(ItemStack::getItemDamage).unconst

/**
 * Interprets the [damage] of this item as a [color](EnumDyeColor). This is only valid for some [item types](itemType),
 * so check that one first.
 */
inline val Const<ItemStack>.color: EnumDyeColor get() = EnumDyeColor.byDyeDamage(damage)

/**
 * Access the underlying NBT data of this item stack in a [Const].
 */
inline val Const<ItemStack>.nbt: Const<NBTTagCompound>?
    get() = unsafeMap(ItemStack::getTagCompound).liftNull()

/**
 * Access the NBT data relating to display data.
 */
inline val Const<ItemStack>.displayNbt: Const<NBTTagCompound>?
    get() = nbt?.getTagCompound("display")

/**
 * Get the display name of this item stack according to its NBT.
 */
fun Const<ItemStack>.getDisplayName(): String? =
    displayNbt?.getString("Name")?.getString()

/**
 * Get the lore of this item stack according to its NBT.
 * @throws NullPointerException if `display.Lore[*]` is not a string in the NBT
 */
fun Const<ItemStack>.getLore(): List<String>? =
    displayNbt
        ?.getList("Lore")
        ?.intoList()
        ?.liftList()
        ?.map { it.tryCast<NBTTagString, NBTBase>()!!.getString() }



