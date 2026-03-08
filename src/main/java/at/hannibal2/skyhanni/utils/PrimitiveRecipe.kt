package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe.NeuRecipeType
import kotlin.time.Duration

interface PrimitiveRecipe {
    val ingredients: Collection<PrimitiveIngredient>
    val outputs: Collection<PrimitiveIngredient>
    val recipeType: NeuRecipeType
    val shouldUseForCraftCost: Boolean
    val output: PrimitiveIngredient?

    fun isCraftingRecipe() = this.recipeType == NeuRecipeType.CRAFTING
}

data class BasePrimitiveRecipe(
    override val ingredients: Collection<PrimitiveIngredient>,
    override val outputs: Collection<PrimitiveIngredient>,
    override val recipeType: NeuRecipeType,
    override val shouldUseForCraftCost: Boolean = recipeType.useForCraftCost,
) : PrimitiveRecipe {
    override val output by lazy { outputs.firstOrNull() }

    fun withDuration(duration: Duration) = DurationPrimitiveRecipe(this, duration)
    fun withMerchant(merchant: NeuInternalName) = MerchantPrimitiveRecipe(this, merchant)
}

data class DurationPrimitiveRecipe(
    val primitiveRecipe: BasePrimitiveRecipe,
    val duration: Duration,
) : PrimitiveRecipe by primitiveRecipe {
    override val output by lazy { outputs.firstOrNull() }
}

data class MerchantPrimitiveRecipe(
    val primitiveRecipe: BasePrimitiveRecipe,
    val merchant: NeuInternalName,
) : PrimitiveRecipe by primitiveRecipe {
    override val output by lazy { outputs.firstOrNull() }
}
