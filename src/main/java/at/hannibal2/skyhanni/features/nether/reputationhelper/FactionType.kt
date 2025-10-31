package at.hannibal2.hanni.features.nether.reputationhelper

enum class FactionType(val factionName: String) {
    BARBARIAN("Barbarian"),
    MAGE("Mage"),
    ;

    companion object {
        fun fromName(name: String) = entries.firstOrNull { it.factionName.equals(name, ignoreCase = true) }
    }
}
