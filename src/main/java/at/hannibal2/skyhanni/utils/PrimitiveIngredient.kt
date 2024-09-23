package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.SKYBLOCK_COIN
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import io.github.moulberry.notenoughupdates.recipes.Ingredient

class PrimitiveIngredient(val internalName: NEUInternalName, val count: Double = 1.0) {

    constructor(internalName: NEUInternalName, count: Int) : this(internalName, count.toDouble())

    constructor(ingredientIdentifier: String) : this(
        ingredientIdentifier.substringBefore(':').asInternalName(),
        ingredientIdentifier.substringAfter(':').toDoubleOrNull() ?: 1.0,
    )

    companion object {
        fun coinIngredient(count: Double = 1.0) = PrimitiveIngredient(SKYBLOCK_COIN, count)

        fun fromNeuIngredient(neuIngredient: Ingredient) = PrimitiveIngredient(neuIngredient.internalItemId.asInternalName(), neuIngredient.count)
    }

    fun isCoin() = internalName == SKYBLOCK_COIN


    override fun toString() = "$internalName x$count"
}
