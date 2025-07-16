package de.hype.bingonet.shared.objects

object SplashLocations {
    val BEA: SplashLocation = SplashLocation("Bea", 26, 70, -93)
    val KAT: SplashLocation = SplashLocation("Kat", 29, 71, -104)
    val ENCHANTING_TABLE: SplashLocation = SplashLocation("Enchanting Table", -33, 69, -109)
    val COMMUNITY_CENTER: SplashLocation = SplashLocation("Community Center", 2, 71, -99)

    @JvmStatic
    fun values(): List<SplashLocation> {
        return listOf(BEA, KAT, ENCHANTING_TABLE, COMMUNITY_CENTER)
    }

    @JvmStatic
    fun getFromExactCoords(position: Position): SplashLocation {
        for (value in values()) {
            if (value.coords == position) {
                return value
            }
        }
        return SplashLocation(position, null)
    }
}
