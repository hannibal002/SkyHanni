package at.hannibal2.skyhanni.data

enum class IslandType(val displayName: String, val mode: String? = null) {
    // TODO USE SH-REPO (for displayName only)
    PRIVATE_ISLAND("Private Island", "dynamic"),
    PRIVATE_ISLAND_GUEST("Private Island Guest"),
    THE_END("The End", "combat_3"),
    KUUDRA_ARENA("Kuudra", "instanced"),
    CRIMSON_ISLE("Crimson Isle", "crimson_isle"),
    DWARVEN_MINES("Dwarven Mines", "mining_3"),
    DUNGEON_HUB("Dungeon Hub", "dungeon_hub"),
    CATACOMBS("Catacombs", "dungeon"),

    HUB("Hub", "hub"),
    DARK_AUCTION("Dark Auction"),
    THE_FARMING_ISLANDS("The Farming Islands", "farming_1"),
    CRYSTAL_HOLLOWS("Crystal Hollows", "crystal_hollows"),
    THE_PARK("The Park", "foraging_1"),
    DEEP_CAVERNS("Deep Caverns", "mining_2"),
    GOLD_MINES("Gold Mine", "mining_1"),
    GARDEN("Garden", "garden"),
    GARDEN_GUEST("Garden Guest"),
    SPIDER_DEN("Spider's Den", "combat_1"),
    WINTER("Jerry's Workshop", "winter"),
    THE_RIFT("The Rift", "rift"),
    MINESHAFT("Mineshaft", "mineshaft"),

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
