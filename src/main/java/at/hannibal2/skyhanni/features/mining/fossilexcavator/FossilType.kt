package at.hannibal2.skyhanni.features.mining.fossilexcavator

enum class FossilType(val totalTiles: Int, val firstPercentage: String) {
    TUSK(8, "12.5%"),
    WEBBED(10, "10%"), //todo confirm there is no decimal
    CLUB(11, "9.1%"),
    SPINE(12, "8.3%"),
    CLAW(13, "7.7%"),
    FOOTPRINT(13, "7.7%"),
    HELIX(14, "7.1%"),
    UGLY(16, "6.2%"), // todo confirm it does round down
    ;

    companion object {
        fun getByPercentage(percentage: String): FossilType? {
            return entries.firstOrNull { it.firstPercentage == percentage }
        }
    }
}
