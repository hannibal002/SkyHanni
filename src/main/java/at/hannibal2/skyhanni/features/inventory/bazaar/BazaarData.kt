package at.hannibal2.skyhanni.features.inventory.bazaar

import at.hannibal2.skyhanni.data.bazaar.BazaarProduct

data class BazaarData(
    val displayName: String,
    val instantBuyPrice: Double,
    val instantSellPrice: Double,
    val product: BazaarProduct,
)
