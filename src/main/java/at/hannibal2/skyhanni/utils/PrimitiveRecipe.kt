package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe.NeuRecipeType
import kotlin.time.Duration

interface PrimitiveRecipe {
    val ingredients: Set<PrimitiveIngredient>
    val outputs: Set<PrimitiveIngredient>
    val recipeType: NeuRecipeType
    val shouldUseForCraftCost: Boolean
    val output: PrimitiveIngredient?

    fun isCraftingRecipe() = this.recipeType == NeuRecipeType.CRAFTING
}

data class BasePrimitiveRecipe(
    override val ingredients: Set<PrimitiveIngredient>,
    override val outputs: Set<PrimitiveIngredient>,
    override val recipeType: NeuRecipeType,
    override val shouldUseForCraftCost: Boolean = true,
) : PrimitiveRecipe {
    override val output by lazy { outputs.firstOrNull() }

    fun withDuration(duration: Duration) = DurationPrimitiveRecipe(this, duration)
}

data class DurationPrimitiveRecipe(
    val primitiveRecipe: BasePrimitiveRecipe,
    val duration: Duration,
) : PrimitiveRecipe by primitiveRecipe {
    override val output by lazy { outputs.firstOrNull() }
}
