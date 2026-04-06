package at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe

import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuItemJson
import at.hannibal2.skyhanni.utils.MerchantPrimitiveRecipe
import com.google.gson.annotations.Expose

data class NeuNpcShopRecipeJson(
    @Expose override val type: NeuRecipeType = NeuRecipeType.NPC_SHOP,
    @Expose val cost: List<NeuRecipeComponent>,
    @Expose val result: NeuRecipeComponent,
) : NeuAbstractRecipe {
    // These cannot be by lazy since this class cannot be KSerializable.
    override val primitiveIngredients get() = cost.mapNotNull { it.toPrimitiveIngredientOrEmpty() }
    private val primitiveOutputs get() = listOf(result.toPrimitiveIngredient())
    override fun getPrimitiveOutputs(itemJson: NeuItemJson) = primitiveOutputs
    override fun getPrimitiveRecipe(itemJson: NeuItemJson): MerchantPrimitiveRecipe =
        super.getBasePrimitiveRecipe(itemJson).withMerchant(itemJson.internalName)
}
