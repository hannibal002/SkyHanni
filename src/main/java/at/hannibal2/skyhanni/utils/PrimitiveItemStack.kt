package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import net.minecraft.item.ItemStack

data class PrimitiveItemStack(val internalName: NEUInternalName, val amount: Int) {

    fun createItem(): ItemStack = internalName.getItemStack().apply { stackSize = amount }

    fun multiply(multiplier: Int): PrimitiveItemStack = PrimitiveItemStack(internalName, amount * multiplier)

    val itemName by lazy { internalName.itemName }

    companion object {

        fun NEUInternalName.makePrimitiveStack(amount: Int = 1) = PrimitiveItemStack(this, amount)
        fun ItemStack.toPrimitiveStackOrNull() = getInternalNameOrNull()?.let { PrimitiveItemStack(it, stackSize) }
    }
}
