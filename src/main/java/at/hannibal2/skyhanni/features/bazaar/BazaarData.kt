package at.hannibal2.skyhanni.features.bazaar

data class BazaarData(
    val apiName: String,
    val itemName: String,
    val sellPrice: Double,
    val buyPrice: Double,
    val npcPrice: Double,
    val buyMovingWeek: Int,
    val sellMovingWeek: Int,
)