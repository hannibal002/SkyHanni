package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.utils.InventoryUtils

object FishingAPI {
    fun hasFishingRodInHand() = InventoryUtils.itemInHandId.asString().contains("ROD")
}
