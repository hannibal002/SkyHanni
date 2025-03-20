package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.event.HandleEvent.Companion.HIGHEST
import at.hannibal2.skyhanni.data.jsonobjects.repo.IslandTypeJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.allLettersFirstUppercase
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

class IslandType(val internalName: String, private val nameFallback: String) {

    // to allow enum like usage
    val name get() = internalName

    init {
        allIslands.add(this)
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

    @SkyHanniModule
    companion object {

        val allIslands = mutableSetOf<IslandType>()

        // to allow enum like usage
        val entries get() = allIslands

        val PRIVATE_ISLAND = IslandType("PRIVATE_ISLAND", "Private Island")
        val PRIVATE_ISLAND_GUEST = IslandType("PRIVATE_ISLAND_GUEST", "Private Island Guest")
        val THE_END = IslandType("THE_END", "The End")
        val KUUDRA_ARENA = IslandType("KUUDRA_ARENA", "Kuudra")
        val CRIMSON_ISLE = IslandType("CRIMSON_ISLE", "Crimson Isle")
        val DWARVEN_MINES = IslandType("DWARVEN_MINES", "Dwarven Mines")
        val DUNGEON_HUB = IslandType("DUNGEON_HUB", "Dungeon Hub")
        val CATACOMBS = IslandType("CATACOMBS", "Catacombs")

        val HUB = IslandType("HUB", "Hub")
        val DARK_AUCTION = IslandType("DARK_AUCTION", "Dark Auction")
        val THE_FARMING_ISLANDS = IslandType("THE_FARMING_ISLANDS", "The Farming Islands")
        val CRYSTAL_HOLLOWS = IslandType("CRYSTAL_HOLLOWS", "Crystal Hollows")
        val THE_PARK = IslandType("THE_PARK", "The Park")
        val DEEP_CAVERNS = IslandType("DEEP_CAVERNS", "Deep Caverns")
        val GOLD_MINES = IslandType("GOLD_MINES", "Gold Mine")
        val GARDEN = IslandType("GARDEN", "Garden")
        val GARDEN_GUEST = IslandType("GARDEN_GUEST", "Garden Guest")
        val SPIDER_DEN = IslandType("SPIDER_DEN", "Spider's Den")
        val WINTER = IslandType("WINTER", "Jerry's Workshop")
        val THE_RIFT = IslandType("THE_RIFT", "The Rift")
        val MINESHAFT = IslandType("MINESHAFT", "Mineshaft")
        val BACKWATER_BAYOU = IslandType("BACKWATER_BAYOU", "Backwater Bayou")

        val NONE = IslandType("NONE", "")
        val ANY = IslandType("ANY", "")
        val UNKNOWN = IslandType("UNKNOWN", "???")

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

        private val islandTypeRepoCheckPattern by RepoPattern.pattern(
            "island-type.repo-check",
            allIslands.joinToString(separator = "|") { it.internalName },
        )

        fun getByName(name: String): IslandType = getByNameOrNull(name) ?: error("IslandType not found: '$name'")
        fun getByNameOrUnknown(name: String): IslandType = getByNameOrNull(name) ?: UNKNOWN
        fun getByNameOrNull(name: String): IslandType? = entries.find { it.displayName == name }

        fun getByIdOrNull(id: String): IslandType? = entries.find { it.islandData?.apiName == id }
        fun getByIdOrUnknown(id: String): IslandType = getByIdOrNull(id) ?: UNKNOWN

        @HandleEvent(priority = HIGHEST)
        fun onRepoReload(event: RepositoryReloadEvent) {
            val data = event.getConstant<IslandTypeJson>("misc/IslandType")

            val islandDataMap = data.islands.mapValues {
                val island = it.value
                IslandData(island.name, island.apiName, island.maxPlayers ?: data.maxPlayers)
            }

            entries.forEach { islandType ->
                islandType.islandData = islandDataMap[islandType.name]
            }

            maxPlayers = data.maxPlayers
            maxPlayersMega = data.maxPlayersMega
        }

        fun getByInternalNameOrNull(name: String): IslandType? = allIslands.firstOrNull { it.internalName == name }

        // used by config type adapter to create a new island type that is not yet known
        fun getByInternalNameOrCreate(name: String): IslandType {
            getByInternalNameOrNull(name)?.let {
                return it
            }

            if (islandTypeRepoCheckPattern.matches(name)) {
                return IslandType(name, name.lowercase().allLettersFirstUppercase())
            }

            ErrorManager.logErrorStateWithData(
                "could not read island type $name",
                "tried to create unknown island type, using unknown instead",
                "name" to name,
            )
            return UNKNOWN
        }
    }
}

data class IslandData(
    val name: String,
    val apiName: String?,
    val maxPlayers: Int,
)
