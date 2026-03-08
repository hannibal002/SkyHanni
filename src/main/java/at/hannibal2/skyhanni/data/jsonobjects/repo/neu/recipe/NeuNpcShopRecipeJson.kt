package at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe

import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuItemJson
import at.hannibal2.skyhanni.utils.BasePrimitiveRecipe
import at.hannibal2.skyhanni.utils.KSerializable
import com.google.gson.annotations.Expose

data class NeuNpcShopRecipeJson(
    @Expose override val type: NeuRecipeType = NeuRecipeType.NPC_SHOP,
    @Expose val cost: List<NeuRecipeComponent>,
    @Expose val result: NeuRecipeComponent,
) : NeuAbstractRecipe {
    private val primitiveInputs get() = cost.mapNotNull { it.toPrimitiveIngredientOrNull() }.toSet()

    override fun getPrimitiveRecipe(itemJson: NeuItemJson): BasePrimitiveRecipe = BasePrimitiveRecipe(
        primitiveInputs,
        listOfNotNull(result.toPrimitiveIngredientOrNull()),
        this.type,
    )
}
