package at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe

import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuItemJson
import at.hannibal2.skyhanni.utils.BasePrimitiveRecipe
import at.hannibal2.skyhanni.utils.PrimitiveIngredient
import at.hannibal2.skyhanni.utils.PrimitiveRecipe

interface NeuAbstractRecipe {
    val type: NeuRecipeType
    val primitiveIngredients: Collection<PrimitiveIngredient>

    fun getPrimitiveRecipe(itemJson: NeuItemJson): PrimitiveRecipe = getBasePrimitiveRecipe(itemJson)
    fun getBasePrimitiveRecipe(itemJson: NeuItemJson): BasePrimitiveRecipe = BasePrimitiveRecipe(
        primitiveIngredients,
        getPrimitiveOutputsFromJson(itemJson),
        this.type,
    )

    fun getPrimitiveOutputs(itemJson: NeuItemJson): Collection<PrimitiveIngredient> = getPrimitiveOutputsFromJson(itemJson)
    fun getPrimitiveOutputsFromJson(itemJson: NeuItemJson, countOverride: Int? = null): Set<PrimitiveIngredient> =
        setOf(PrimitiveIngredient(itemJson.internalName, countOverride ?: 1))
}
