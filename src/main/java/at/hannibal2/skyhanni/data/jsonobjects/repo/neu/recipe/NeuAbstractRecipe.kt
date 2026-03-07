package at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe

import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuItemJson
import at.hannibal2.skyhanni.utils.PrimitiveIngredient
import at.hannibal2.skyhanni.utils.PrimitiveRecipe

abstract class NeuAbstractRecipe<P : PrimitiveRecipe> {
    abstract val type: NeuRecipeType

    abstract fun getPrimitiveRecipe(itemJson: NeuItemJson): P

    internal open fun getPrimitiveOutputs(itemJson: NeuItemJson, countOverride: Int? = null): Set<PrimitiveIngredient> = setOf(
        PrimitiveIngredient(itemJson.internalName, countOverride ?: 1)
    )
}
