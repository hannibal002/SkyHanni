package at.hannibal2.skyhanni.features.inventory.bazaar

import at.hannibal2.skyhanni.data.bazaar.BazaarProduct

data class BazaarData(
    val displayName: String,
    val sellOfferPrice: Double,
    val instantBuyPrice: Double,
    val product: BazaarProduct,
    @Deprecated("outdated", ReplaceWith("instantBuyPrice"))
    val sellPrice: Double = instantBuyPrice,
    @Deprecated("outdated", ReplaceWith("sellOfferPrice"))
    val buyPrice: Double = sellOfferPrice,
)
