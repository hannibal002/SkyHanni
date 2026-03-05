package at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe

import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.PrimitiveIngredient

data class NeuRecipeComponent(val internalName: NeuInternalName?, val count: Int = 1) {
    fun toJsonString() = internalName?.let { "$it:$count" }.orEmpty()

    companion object {
        val EMPTY = NeuRecipeComponent(null, 0)

        fun fromJsonStringOrNull(component: String): NeuRecipeComponent? {
            if (component.isEmpty()) return null
            val parts = component.split(":")
            val internalName = parts.firstOrNull()?.toInternalName() ?: return null
            val quantity = parts.getOrNull(1)?.toIntOrNull() ?: 1
            return NeuRecipeComponent(internalName, quantity)
        }
    }

    fun toPrimitiveIngredientOrNull() = internalName?.let { PrimitiveIngredient(it, count) }
    fun toPrimitiveIngredient(countOverride: Int? = null) = PrimitiveIngredient(
        internalName ?: error("Internal name cannot be null"),
        countOverride ?: count
    )
}
