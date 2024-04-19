package at.hannibal2.skyhanni.features.garden.fortuneguide

data class FortuneUpgrade(
    val description: String,
    val costCopper: Int?,
    //todo make into NEUInternalName
    val requiredItem: String,
    val itemQuantity: Int,
    val fortuneIncrease: Double,
    var cost: Int? = null,
    var costPerFF: Int? = null, // also the same as time to repay
)
