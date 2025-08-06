package at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe

import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuItemJson
import at.hannibal2.skyhanni.utils.PrimitiveIngredient

abstract class NeuAbstractRecipe {
    abstract val type: NeuRecipeType

    abstract fun getPrimitiveInputs(itemJson: NeuItemJson): List<PrimitiveIngredient>

    open fun getPrimitiveOutputs(itemJson: NeuItemJson): List<PrimitiveIngredient> = listOf(
        getPrimitiveOutput(itemJson)
    )

    protected open val outputOverride: NeuOverrideProvider? = null

    private fun getPrimitiveOutput(itemJson: NeuItemJson): PrimitiveIngredient {
        val craftAmount = outputOverride?.overrideCount ?: 1
        val outputInternalName = outputOverride?.overrideItem ?: itemJson.internalName
        return PrimitiveIngredient(outputInternalName, craftAmount)
    }
}
