package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class ItemDiscountsJson(
    @Expose @SerializedName("item_price_coin_only") val itemPriceCoinOnly: Map<String, Int>,
    @Expose @SerializedName("items_to_discount_by_area") val itemsToDiscountByArea: ItemsToDiscountByArea,
    @Expose @SerializedName("scaling_discounts") val scalingDiscounts: ScalingDiscounts
)

data class ItemsToDiscountByArea(
    @Expose val Emissary: List<String>
)

data class ScalingDiscounts(
    @Expose val Emissary: Map<String, Double>
)
