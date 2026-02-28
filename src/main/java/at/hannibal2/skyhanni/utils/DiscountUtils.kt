package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.CrimsonIsleReputationApi
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.jsonobjects.repo.ItemDiscountsJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getRecipePrice
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getRecipes
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.mutableMapOf

@SkyHanniModule
object DiscountUtils {

    private var itemPriceCoinOnly = mutableMapOf<NeuInternalName, Int>()
    private var emissaryItems = mutableListOf<NeuInternalName>()
    private var emissaryScalingDiscounts = mutableMapOf<Int, Double>()

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<ItemDiscountsJson>("misc/ItemDiscounts")
        data.itemPriceCoinOnly.forEach { (string, coins) ->
            itemPriceCoinOnly[string.toInternalName()] = coins
        }
        data.itemsToDiscountByArea.Emissary.forEach {
            emissaryItems.add(it.toInternalName())
        }
        data.scalingDiscounts.Emissary.forEach { (reputation, discount) ->
            emissaryScalingDiscounts[reputation.toInt()] = discount
        }
    }

    fun NeuInternalName.getDiscountedPrice(
        priceSource: ItemPriceSource = ItemPriceSource.BAZAAR_INSTANT_BUY,
        pastRecipes: List<PrimitiveRecipe> = emptyList(),
    ): Double {
        val lowestNPCPrice = getRecipes(this).filter { it.recipeType != RecipeType.NPC_SHOP && it !in pastRecipes }
            .map { it.getRecipePrice(priceSource, pastRecipes + it) }
            .filter { it > 0 }
            .minOrNull() ?: return 0.0
        if (!emissaryItems.contains(this)) {
            val rep = ProfileStorageData.profileSpecific?.crimsonIsleReputation?.maxBy { it.value }?.value ?: 0
            var itemDiscount = 1.0
            emissaryScalingDiscounts.forEach { (reputation, discount) ->
                if (rep > reputation) itemDiscount = (1.0 - discount)
            }
            val priceDecrease = itemPriceCoinOnly[this]?.times(itemDiscount) ?: 0.0
            return lowestNPCPrice - priceDecrease
        }
        return lowestNPCPrice
    }
}
