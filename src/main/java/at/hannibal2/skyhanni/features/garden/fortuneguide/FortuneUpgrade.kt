package at.hannibal2.skyhanni.features.garden.fortuneguide

data class FortuneUpgrade(
    val description: String,
    val costCopper: Int?,
    val requiredItem: String?,
    val itemQuantity: Int?,
    val fortuneIncrease: Double
)