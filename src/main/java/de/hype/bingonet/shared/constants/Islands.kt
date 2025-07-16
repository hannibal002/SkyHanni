package de.hype.bingonet.shared.constants

/**
 * List of all Islands
 * [.CRYSTAL_HOLLOWS]
 * [.CRIMSON_ISLE]
 * [.DEEP_CAVERNS]
 * [.DUNGEON]
 * [.DUNGEON_HUB]
 * [.DWARVEN_MINES]
 * [.GOLD_MINE]
 * [.HUB]
 * [.KUUDRA]
 * [.PRIVATE_ISLAND]
 * [.SPIDERS_DEN]
 * [.THE_END]
 * [.THE_FARMING_ISLANDS]
 * [.JERRYS_WORKSHOP]
 * [.THE_RIFT]
 */
enum class Islands(val internalName: String, private val displayName: String) {
    CRYSTAL_HOLLOWS("crystal_hollows", "Crystal Hollows", "nucleus"),
    CRIMSON_ISLE("crimson_isle", "Crimson Isle", "crimson"),
    DEEP_CAVERNS("mining_2", "Deep Caverns", "deep"),
    DUNGEON("dungeon", "Dungeon"),
    DUNGEON_HUB("dungeon_hub", "Dungeon Hub", "dhub"),
    DWARVEN_MINES("mining_3", "Dwarven Mines", "mines"),
    GOLD_MINE("mining_1", "Gold Mine", "gold"),
    HUB("hub", "Hub", "hub"),
    GLACITE_TUNNEL("mineshaft", "Mineshaft"),
    KUUDRA("kuudra", "Kuudra"),
    PRIVATE_ISLAND("dynamic", "Private Island", "home"),
    SPIDERS_DEN("combat_1", "Spider's Den", "spider"),
    THE_END("combat_3", "The End", "end"),
    THE_FARMING_ISLANDS("farming_1", "The Farming Islands", "barn"),
    JERRYS_WORKSHOP("winter", "Jerry's Workshop"),
    THE_RIFT("rift", "The Rift"),
    THE_PARK("foraging_1", "The Park", "park"),
    DARK_AUCTION("dark_auction", "Dark Auction"),
    BAYOU("fishing_1", "Backwater Bayou", "bayou"),
    ;

    constructor(internalName: String, displayName: String, warpArgument: String?) : this(internalName, displayName) {
        this.warpArgument = warpArgument
    }

    var warpArgument: String? = null
        private set

    fun getDisplayName(): String {
        return displayName
    }

    companion object {
        @JvmStatic
        fun getIslandByMap(map: String): Islands? {
            for (island in entries) {
                if (island.displayName == map) {
                    return island
                }
            }
            return null
        }
    }
}
