package at.hannibal2.skyhanni.data

enum class IslandType(val displayName: String) {
    // TODO USE SH-REPO (for displayName only)
    PRIVATE_ISLAND("Private Island"),
    PRIVATE_ISLAND_GUEST("Private Island Guest"),
    THE_END("The End"),
    KUUDRA_ARENA("Kuudra"),
    CRIMSON_ISLE("Crimson Isle"),
    DWARVEN_MINES("Dwarven Mines"),
    DUNGEON_HUB("Dungeon Hub"),
    CATACOMBS("Catacombs"),

    HUB("Hub"),
    DARK_AUCTION("Dark Auction"),
    THE_FARMING_ISLANDS("The Farming Islands"),
    CRYSTAL_HOLLOWS("Crystal Hollows"),
    THE_PARK("The Park"),
    DEEP_CAVERNS("Deep Caverns"),
    GOLD_MINES("Gold Mine"),
    GARDEN("Garden"),
    GARDEN_GUEST("Garden Guest"),
    SPIDER_DEN("Spider's Den"),
    WINTER("Jerry's Workshop"),
    THE_RIFT("The Rift"),
    MINESHAFT("Mineshaft"),

    NONE(""),
    ANY(""),
    UNKNOWN("???"),
    ;

    companion object {

        fun getByNameOrUnknown(name: String) = getByNameOrNull(name) ?: UNKNOWN
        fun getByName(name: String) = getByNameOrNull(name) ?: error("IslandType not found: '$name'")

        fun getByNameOrNull(name: String) = entries.firstOrNull { it.displayName == name }
    }
}
