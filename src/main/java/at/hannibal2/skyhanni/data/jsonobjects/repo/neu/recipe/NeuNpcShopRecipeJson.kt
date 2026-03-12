package at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe

import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuItemJson
import com.google.gson.annotations.Expose

data class NeuNpcShopRecipeJson(
    @Expose override val type: NeuRecipeType = NeuRecipeType.NPC_SHOP,
    @Expose val cost: List<NeuRecipeComponent>,
    @Expose val result: NeuRecipeComponent,
) : NeuAbstractRecipe() {
    override fun getPrimitiveInputs(itemJson: NeuItemJson) = cost.mapNotNull {
        it.toPrimitiveIngredientOrNull()
    }
    override val outputOverride: NeuOverrideProvider = NeuOverrideProvider(result)
}
