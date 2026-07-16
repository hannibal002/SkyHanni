package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod.launch
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.event.HandleEvent.Companion.HIGHEST
import at.hannibal2.skyhanni.data.jsonobjects.repo.IslandTypeJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LocationUtils.isInside
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import net.minecraft.world.phys.AABB

enum class IslandType(private val nameFallback: String, private val apiNameFallback: String?) {
    // General
    PRIVATE_ISLAND("Private Island", "dynamic"),
    PRIVATE_ISLAND_GUEST("Private Island Guest", null),
    HUB("Hub", "hub"),
    DARK_AUCTION("Dark Auction", "dark_auction"),
    WINTER("Jerry's Workshop", "winter"),

    // Farming
    THE_FARMING_ISLANDS("The Farming Islands", "farming_1"),
    GARDEN("Garden", "garden"),
    GARDEN_GUEST("Garden Guest", null),

    // Mining
    GOLD_MINES("Gold Mine", "mining_1"),
    DEEP_CAVERNS("Deep Caverns", "mining_2"),
    DWARVEN_MINES("Dwarven Mines", "mining_3"),
    CRYSTAL_HOLLOWS("Crystal Hollows", "crystal_hollows"),
    MINESHAFT("Mineshaft", "mineshaft"),

    // Fishing
    BACKWATER_BAYOU("Backwater Bayou", "fishing_1"),
    LOTUS_ATOLL("Lotus Atoll", "lotus_atoll"),

    // Foraging
    THE_PARK("The Park", "foraging_1"),
    GALATEA("Galatea", "foraging_2"),
    TORRHUS_CANYON("Torrhus Canyon", "foraging_3"),

    // Combat
    SPIDER_DEN("Spider's Den", "combat_1"),
    THE_END("The End", "combat_3"),
    CRIMSON_ISLE("Crimson Isle", "crimson_isle"),

    // Dungeons
    DUNGEON_HUB("Dungeon Hub", "dungeon_hub"),
    CATACOMBS("Catacombs", "dungeon"),
    KUUDRA_ARENA("Kuudra", "kuudra"),

    // Special
    THE_RIFT("The Rift", "rift"),
    SAFARI("Safari", "safari"),

    // Special values
    NONE("", null),
    ANY("", null),
    UNKNOWN("???", null),
    ;

    fun isValidIsland(): Boolean = when (this) {
        NONE,
        ANY,
        UNKNOWN,
        -> false

        else -> true
    }

    fun guestVariant(): IslandType = when (this) {
        PRIVATE_ISLAND -> PRIVATE_ISLAND_GUEST
        GARDEN -> GARDEN_GUEST
        else -> this
    }

    // TODO: IslandTags
    fun hasGuestVariant(): Boolean = when (this) {
        PRIVATE_ISLAND, GARDEN -> true
        else -> false
    }

    var islandData: IslandData? = null
        private set

    val displayName: String get() = islandData?.name ?: nameFallback

    val apiName: String? get() = islandData?.apiName ?: apiNameFallback

    fun isInBounds(vec: LorenzVec): Boolean = islandData?.boundingBox?.isInside(vec) ?: true

    @SkyHanniModule
    companion object {

        fun Collection<IslandType>.isInAnyIsland(): Boolean = any { it.isInIsland() }
        private val repoReloadCoroutine = CoroutineSettings("island type repo reload")

        /**
         * The maximum amount of players that can be on an island.
         */
        var maxPlayers = 24
            private set

        /**
         * The maximum amount of players that can be on a mega hub.
         */
        var maxPlayersMega = 80
            private set

        fun getByName(name: String): IslandType = getByNameOrNull(name) ?: error("IslandType not found: '$name'")
        fun getByNameOrUnknown(name: String): IslandType = getByNameOrNull(name) ?: UNKNOWN
        fun getByNameOrNull(name: String): IslandType? = entries.find { it.displayName == name }

        fun getByIdOrNull(id: String): IslandType? = entries.find { it.apiName == id }
        fun getByIdOrUnknown(id: String): IslandType = getByIdOrNull(id) ?: UNKNOWN

        @HandleEvent(priority = HIGHEST)
        fun onRepoReload(event: RepositoryReloadEvent) = repoReloadCoroutine.launch {
            val data = event.getConstantAsync<IslandTypeJson>("misc/IslandType")

            entries.forEach { islandType ->
                islandType.islandData = data.islands[islandType.name]?.let { island ->
                    val boundingBox = island.bounds?.let { bounds ->
                        AABB(
                            bounds.minX.toDouble(), 0.0, bounds.minZ.toDouble(),
                            bounds.maxX.toDouble(), 256.0, bounds.maxZ.toDouble(),
                        )
                    }
                    IslandData(island.name, island.apiName, island.maxPlayers ?: data.maxPlayers, boundingBox)
                }
            }

            maxPlayers = data.maxPlayers
            maxPlayersMega = data.maxPlayersMega
        }
    }

    fun isInIsland() = SkyBlockUtils.inSkyBlock && SkyBlockUtils.currentIsland == this
}

data class IslandData(
    val name: String,
    val apiName: String?,
    val maxPlayers: Int,
    val boundingBox: AABB?,
)
