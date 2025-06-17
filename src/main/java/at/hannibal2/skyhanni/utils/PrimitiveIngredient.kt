package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import net.minecraft.item.ItemStack

class PrimitiveIngredient(val internalName: NeuInternalName, val count: Double = 1.0) {

    constructor(internalName: NeuInternalName, count: Int) : this(internalName, count.toDouble())

    constructor(ingredientIdentifier: String) : this(
        ingredientIdentifier.substringBefore(':').toInternalName(),
        // if second part is blank, the count is assumed to be 1
        ingredientIdentifier.substringAfter(':', "").let { if (it.isBlank()) 1.0 else it.formatDouble() },
    )

    companion object {
        fun coinIngredient(count: Double = 1.0) = PrimitiveIngredient(NeuInternalName.SKYBLOCK_COIN, count)

        fun Set<PrimitiveIngredient>.toPrimitiveItemStacks(): List<PrimitiveItemStack> =
            map { it.toPrimitiveItemStack() }

        fun ItemStack?.toPrimitiveIngredient(): PrimitiveIngredient {
            val internalName = this?.getInternalNameOrNull() ?: NeuInternalName.NONE
            return PrimitiveIngredient(internalName, this?.stackSize?.toDouble() ?: 1.0)
        }
    }

    fun isCoin() = internalName == NeuInternalName.SKYBLOCK_COIN

    override fun toString() = "$internalName x$count"

    fun asRepoString(): String {
        if (internalName == NeuInternalName.NONE) return ""
        return "${internalName.asString()}:${count.toInt()}".removeSuffix(":1")
    }

    fun toPair() = Pair(internalName, count)

    // TODO should maybe throw an error when trying to use with internalName == SKYBLOCK_COIN
    fun toPrimitiveItemStack() = PrimitiveItemStack(internalName, count.toInt())
}
