package at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe

import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.PrimitiveIngredient
import at.hannibal2.skyhanni.utils.json.SkyHanniAdaptable

data class NeuRecipeComponent(
    val internalName: NeuInternalName?,
    val count: Int = 1,
) : SkyHanniAdaptable<NeuRecipeComponent> {
    override fun toJsonString() = internalName?.let { "$it:$count" }.orEmpty()

    companion object : SkyHanniAdaptable.Factory<NeuRecipeComponent> {
        override fun fromJsonString(json: String): NeuRecipeComponent =
            fromJsonStringOrNull(json) ?: NeuRecipeComponent(null, 0)

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
