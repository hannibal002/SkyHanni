package at.hannibal2.hanni.utils

import at.hannibal2.hanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.hanni.utils.ItemUtils.repoItemName
import at.hannibal2.hanni.utils.NeuItems.getItemStack
import net.minecraft.item.ItemStack

data class PrimitiveItemStack(val internalName: NeuInternalName, val amount: Int) {

    fun createItem(): ItemStack = internalName.getItemStack().apply { stackSize = amount }

    operator fun times(multiplier: Int): PrimitiveItemStack = PrimitiveItemStack(internalName, amount * multiplier)

    operator fun plus(amount: Int): PrimitiveItemStack = PrimitiveItemStack(internalName, this.amount + amount)

    val itemName by lazy { internalName.repoItemName }

    fun toPair() = Pair(internalName, amount)

    fun toPrimitiveIngredient() = PrimitiveIngredient(internalName, amount.toDouble())

    companion object {

        fun NeuInternalName.makePrimitiveStack(amount: Int = 1) = PrimitiveItemStack(this, amount)
        fun ItemStack.toPrimitiveStackOrNull() = getInternalNameOrNull()?.let { PrimitiveItemStack(it, stackSize) }
    }
}
