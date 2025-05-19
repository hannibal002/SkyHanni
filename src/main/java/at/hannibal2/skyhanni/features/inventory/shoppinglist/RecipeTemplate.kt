package at.hannibal2.skyhanni.features.inventory.shoppinglist

import at.hannibal2.skyhanni.features.inventory.shoppinglist.IngredientTemplate.Companion.toIngredientTemplate
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.PrimitiveRecipe
import com.google.gson.annotations.Expose

class RecipeTemplate(
    @Expose val ingredients: MutableList<IngredientTemplate> = mutableListOf<IngredientTemplate>(),
    @Expose val result: IngredientTemplate?,
) {

    fun toPrimitiveRecipe(): PrimitiveRecipe? {
        val ingredients = ingredients.map { it.toPrimitiveIngredient() }
        val result = result?.toPrimitiveIngredient()

        val possibleRecipes = NeuItems.getRecipes(result?.internalName ?: return null)

        for (recipe in possibleRecipes) {
            if (recipe.ingredients.containsAll(ingredients)) {
                return recipe
            }
        }
        return null
    }

    companion object {
        fun PrimitiveRecipe.toRecipeTemplate(): RecipeTemplate {
            return RecipeTemplate(ingredients.map { it.toIngredientTemplate() }.toMutableList(), output?.toIngredientTemplate())
        }
    }
}
