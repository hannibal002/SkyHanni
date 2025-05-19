package at.hannibal2.skyhanni.features.inventory.shoppinglist

import at.hannibal2.skyhanni.features.inventory.shoppinglist.RecipeTemplate.Companion.toRecipeTemplate
import at.hannibal2.skyhanni.utils.NeuInternalName
import com.google.gson.annotations.Expose

class ItemTemplate(
    @Expose val internalName: String,
    @Expose val amount: Double,
    @Expose val hidden: Boolean,
    @Expose val recipe: RecipeTemplate?,
    @Expose val subItems: List<ItemTemplate>,
) {

    fun toShoppingListItem(topLevelCategory: ShoppingListCategory, topLevelItem: ShoppingListItem? = null): ShoppingListItem {
        val result = ShoppingListItem(
            NeuInternalName.fromItemNameOrInternalName(this.internalName),
            this.amount,
            topLevelCategory,
            topLevelItem,
            this.recipe?.toPrimitiveRecipe(),
            this.hidden,
        )
        this.subItems.forEach {
            result.subItems.add(it.toShoppingListItem(topLevelCategory, result))
        }

        val ingredients: MutableMap<NeuInternalName, Double> = mutableMapOf()
        result.recipe?.ingredients?.forEach {
            ingredients[it.internalName] = it.count / (result.recipe?.output?.count ?: 1.0)
        }

        result.subItems.forEach {
            if (it.internalName in ingredients && ingredients[it.internalName] != null) {
                it.amount = ingredients[it.internalName] ?: 1.0
            }
        }
        return result
    }

    companion object {
        fun ShoppingListItem.toItemTemplate(): ItemTemplate {
            return ItemTemplate(internalName.asString(), amount, hidden, recipe?.toRecipeTemplate(), subItems.map { it.toItemTemplate() })
        }
    }
}
