package at.hannibal2.skyhanni.data

enum class IslandType(val displayName: String) {
    PRIVATE_ISLAND("Private Island"),
    PRIVATE_ISLAND_GUEST("Private Island Guest"),
    THE_END("The End"),
    KUUDRA_ARENA("Instanced"),
    CRIMSON_ISLE("Crimson Isle"),
    DWARVEN_MINES("Dwarven Mines"),
    DUNGEON_HUB("Dungeon Hub"),

    HUB("Hub"),
    THE_FARMING_ISLANDS("The Farming Islands"),

    NONE(""),
    UNKNOWN("???"),
    ;

    companion object {
        fun getBySidebarName(name: String): IslandType {
            return values().firstOrNull { it.displayName == name } ?: UNKNOWN
        }
    }
}