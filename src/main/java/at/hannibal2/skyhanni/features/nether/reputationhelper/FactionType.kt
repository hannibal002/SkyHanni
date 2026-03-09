package at.hannibal2.skyhanni.features.nether.reputationhelper

enum class FactionType(val factionName: String, val apiName: String) {
    BARBARIAN("Barbarian", "barbarians"),
    MAGE("Mage", "mages"),
    ;

    companion object {
        fun fromName(name: String): FactionType? = entries.firstOrNull { it.factionName.equals(name, ignoreCase = true) }
        fun fromAPIName(name: String): FactionType? = entries.firstOrNull { it.apiName.equals(name, ignoreCase = true) }
    }
}
