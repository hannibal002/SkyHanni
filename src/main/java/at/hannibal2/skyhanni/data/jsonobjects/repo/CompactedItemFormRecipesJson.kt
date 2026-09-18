package at.hannibal2.skyhanni.data.jsonobjects.repo

import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe.NeuRecipeType
import at.hannibal2.skyhanni.utils.NeuInternalName
import com.google.gson.annotations.Expose

data class CompactedItemFormRecipesJson(
    @Expose val recipes: Map<NeuInternalName, CompactedItemFormRecipeEntry>,
)

data class CompactedItemFormRecipeEntry(
    @Expose val type: NeuRecipeType,
    @Expose val result: NeuInternalName,
)
