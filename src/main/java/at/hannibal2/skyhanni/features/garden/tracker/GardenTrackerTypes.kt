package at.hannibal2.skyhanni.features.garden.tracker

enum class GardenTrackerTypes(val displayName: String) {
    BREAKING_CROPS("Breaking Crops"),
    PESTS("Pests"),
    COMPOSTER("Composter"),
    VISITORS("Visitors"),
    CONSUMABLES("Consumables"),
    ;

    override fun toString(): String = displayName

}
