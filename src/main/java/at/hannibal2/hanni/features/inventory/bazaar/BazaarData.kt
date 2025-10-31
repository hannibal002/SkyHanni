package at.hannibal2.hanni.features.inventory.bazaar

import at.hannibal2.hanni.data.bazaar.BazaarProduct

data class BazaarData(
    val displayName: String,
    val instantBuyPrice: Double,
    val instantSellPrice: Double,
    val product: BazaarProduct,
)
