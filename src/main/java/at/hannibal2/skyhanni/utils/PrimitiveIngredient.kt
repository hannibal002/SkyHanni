package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.SKYBLOCK_COIN
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import io.github.moulberry.notenoughupdates.recipes.Ingredient

class PrimitiveIngredient(val internalName: NEUInternalName, val count: Int = 1) {

    constructor(ingredientIdentifier: String) : this(
        ingredientIdentifier.substringBefore(':').asInternalName(),
        ingredientIdentifier.substringAfter(':').formatInt(),
    )

    companion object {
        fun coinIngredient(count: Double = 1.0) = PrimitiveIngredient(SKYBLOCK_COIN, count.toInt())

        fun fromNeuIngredient(neuIngredient: Ingredient) =
            PrimitiveIngredient(neuIngredient.internalItemId.asInternalName(), neuIngredient.count.toInt())
    }

    fun isCoin() = internalName == SKYBLOCK_COIN

    override fun toString() = "$internalName x$count"

    fun toPair() = Pair(internalName, count)
    fun toPrimitiveItemStack() = PrimitiveItemStack(internalName, count)
}
