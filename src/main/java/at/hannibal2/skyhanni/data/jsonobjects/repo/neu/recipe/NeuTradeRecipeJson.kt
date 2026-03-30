package at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe

import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuItemJson
import at.hannibal2.skyhanni.utils.KSerializable
import com.google.gson.annotations.Expose

@KSerializable
data class NeuTradeRecipeJson(
    @Expose override val type: NeuRecipeType = NeuRecipeType.TRADE,
    @Expose private val cost: NeuRecipeComponent,
    @Expose private val min: Int? = null,
    @Expose private val max: Int? = null,
    @Expose val result: NeuRecipeComponent,
) : NeuAbstractRecipe {
    private val costCount = when {
        min != null && max != null -> (min + max) / 2
        else -> cost.count
    }
    override val primitiveIngredients by lazy { listOf(cost.toPrimitiveIngredient(costCount)) }
    private val primitiveOutput by lazy { listOf(result.toPrimitiveIngredient()) }
    override fun getPrimitiveOutputs(itemJson: NeuItemJson) = primitiveOutput
}
