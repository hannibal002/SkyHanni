package at.hannibal2.skyhanni.features.inventory.shoppinglist

import at.hannibal2.skyhanni.features.inventory.shoppinglist.ItemTemplate.Companion.toItemTemplate
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzColor.Companion.toLorenzColor
import com.google.gson.annotations.Expose

class CategoryTemplate(
    @Expose val name: String,
    @Expose val color: Char,
    @Expose val hidden: Boolean,
    @Expose val items: List<ItemTemplate>,
) {

    fun toShoppingListCategory(): ShoppingListCategory {
        val result = ShoppingListCategory(name, color.toLorenzColor() ?: LorenzColor.GOLD, hidden)
        items.forEach {
            result.items.add(it.toShoppingListItem(result))
        }
        return result
    }

    companion object {
        fun ShoppingListCategory.toCategoryTemplate(): CategoryTemplate {
            return CategoryTemplate(name, color.chatColorCode, hidden, items.map { it.toItemTemplate() })
        }
    }

    override fun toString(): String {
        return "CategoryTemplate $name (${items.size} items)"
    }
}
