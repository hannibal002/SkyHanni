package at.hannibal2.skyhanni.utils

import io.github.moulberry.notenoughupdates.recipes.Ingredient

class PrimitiveIngredient(val internalName: String, val count: Double = 1.0) {

    constructor(internalName: String, count: Int) : this(internalName, count.toDouble())

    constructor(ingredientIdentifier: String) : this(
        ingredientIdentifier.substringBefore(':'),
        ingredientIdentifier.substringAfter(':').toDoubleOrNull() ?: 1.0,
    )

    companion object {
        const val SKYBLOCK_COIN = "SKYBLOCK_COIN"

        fun coinIngredient(count: Double = 1.0) = PrimitiveIngredient(SKYBLOCK_COIN, count)

        fun fromNeuIngredient(neuIngredient: Ingredient) = PrimitiveIngredient(neuIngredient.internalItemId, neuIngredient.count)
    }

    fun isCoin() = internalName == SKYBLOCK_COIN


    override fun toString() = "$internalName x$count"
}
