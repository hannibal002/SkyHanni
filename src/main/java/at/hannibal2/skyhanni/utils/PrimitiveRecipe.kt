package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe.NeuRecipeType

data class PrimitiveRecipe(
    val ingredients: Set<PrimitiveIngredient>,
    val outputs: Set<PrimitiveIngredient>,
    val recipeType: NeuRecipeType,
    val shouldUseForCraftCost: Boolean = true,
) {
    val output by lazy { outputs.firstOrNull() }

    fun isCraftingRecipe() = this.recipeType == NeuRecipeType.CRAFTING
}
