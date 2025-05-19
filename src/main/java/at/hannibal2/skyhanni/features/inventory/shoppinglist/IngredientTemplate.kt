package at.hannibal2.skyhanni.features.inventory.shoppinglist

import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.PrimitiveIngredient
import com.google.gson.annotations.Expose

class IngredientTemplate(
    @Expose val internalName: String,
    @Expose val amount: Double,
) {

    fun toPrimitiveIngredient() = PrimitiveIngredient(internalName.toInternalName(), amount)

    companion object {
        fun PrimitiveIngredient.toIngredientTemplate() = IngredientTemplate(internalName.toString(), count)
    }
}
