package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.SKYBLOCK_COIN
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import io.github.moulberry.notenoughupdates.recipes.Ingredient

class PrimitiveIngredient(val internalName: NEUInternalName, val count: Double = 1.0) {

    constructor(ingredientIdentifier: String) : this(
        ingredientIdentifier.substringBefore(':').asInternalName(),
        ingredientIdentifier.substringAfter(':').formatDouble(),
    )

    companion object {
        fun coinIngredient(count: Double = 1.0) = PrimitiveIngredient(SKYBLOCK_COIN, count)

        fun fromNeuIngredient(neuIngredient: Ingredient) =
            PrimitiveIngredient(neuIngredient.internalItemId.asInternalName(), neuIngredient.count)
    }

    fun isCoin() = internalName == SKYBLOCK_COIN

    override fun toString() = "$internalName x$count"

    fun toPair() = Pair(internalName, count)

    // TODO should maybe throw an error when trying to use with internalName == SKYBLOCK_COIN
    fun toPrimitiveItemStack() = PrimitiveItemStack(internalName, count.toInt())
}
