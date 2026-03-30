package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble

class PrimitiveIngredient(val internalName: NeuInternalName, val count: Double = 1.0) {

    constructor(internalName: NeuInternalName, count: Int) : this(internalName, count.toDouble())

    constructor(ingredientIdentifier: String) : this(
        ingredientIdentifier.substringBefore(':').toInternalName(),
        // if second part is blank, the count is assumed to be 1
        ingredientIdentifier.substringAfter(':', "").let { if (it.isBlank()) 1.0 else it.formatDouble() },
    )

    companion object {
        fun Collection<PrimitiveIngredient>.toPrimitiveItemStacks(): List<PrimitiveItemStack> =
            map { it.toPrimitiveItemStack() }

        val EMPTY by lazy { PrimitiveIngredient(NeuInternalName.NONE, 0.0) }
    }

    override fun toString() = "$internalName x$count"

    // TODO should maybe throw an error when trying to use with internalName == SKYBLOCK_COIN
    fun toPrimitiveItemStack() = PrimitiveItemStack(internalName, count.toInt())
}
