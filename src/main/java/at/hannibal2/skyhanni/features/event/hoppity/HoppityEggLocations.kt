package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.jsonobjects.repo.HoppityEggLocationsJson
import at.hannibal2.skyhanni.events.NeuProfileDataLoadedEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.ChocolateFactoryAPI
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object HoppityEggLocations {

    val currentIslandLocations
        get() = eggLocations[LorenzUtils.skyBlockIsland]

    // TODO: only use apiEggLocations
    private var apiEggLocations: Map<IslandType, Map<String, LorenzVec>> = mapOf()
    private var eggLocations: Map<IslandType, List<LorenzVec>> = mapOf()

    private var collectedEggLocations: MutableMap<IslandType, MutableSet<LorenzVec>>?
        get() = ChocolateFactoryAPI.profileStorage?.collectedEggLocations
        set(value) {
            ChocolateFactoryAPI.profileStorage?.collectedEggLocations = value
        }

    private val collectedLocationCount
        get() = collectedEggLocations?.values?.sumOf { it.size } ?: 0


    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        // TODO: split Chocolate Factory and Hoppity repo data
        val data = event.getConstant<HoppityEggLocationsJson>("HoppityEggLocations")
        apiEggLocations = data.apiEggLocations
        eggLocations = data.eggLocations
    }

    fun saveNearestEgg() {
        val location = currentIslandLocations
            ?.minByOrNull { it.distanceSqToPlayer() } ?: return
        if (location.distanceSqToPlayer() > 100) {
            ErrorManager.skyHanniError(
                "Player far from any known egg location!",
                "island" to LorenzUtils.skyBlockIsland,
                "distanceSqToPlayer" to location.distanceSqToPlayer(),
                "playerLocation" to LocationUtils.playerLocation(),
                "closestKnownEgg" to location,
            )
        }

        currentIslandCollectedEggs()?.add(location)
    }

    private fun currentIslandCollectedEggs(): MutableSet<LorenzVec>? =
        collectedEggLocations?.getOrPut(LorenzUtils.skyBlockIsland) { mutableSetOf() }



    private var loadedNeuThisProfile = false

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        loadedNeuThisProfile = false
    }

    @SubscribeEvent
    fun onNeuProfileDataLoaded(event: NeuProfileDataLoadedEvent) {
        if (loadedNeuThisProfile || !HoppityEggsManager.config.loadFromNeuPv) return

        // optional chaining to hopefully catch any API responses missing data
        val rawLocations = event.getCurrentPlayerData()?.events?.easter?.rabbits?.collectedLocations ?: return

        val apiCollectedLocations = rawLocations.values.flatten()

        val result = mutableMapOf<IslandType, MutableSet<LorenzVec>>()

        for ((island, locationNameToCoords) in apiEggLocations) {
            val coords = apiCollectedLocations.mapNotNull { locationNameToCoords[it] }
            result[island] = coords.toMutableSet()
        }

        // don't load if there's no change
        if (apiCollectedLocations.size == collectedLocationCount) return

        ChatUtils.clickableChat(
            message = "Click here to load ${apiCollectedLocations.size} collected egg locations from NEU PV!",
            onClick = {
                collectedEggLocations = result
                ChatUtils.chat("Updated Hoppity egg location data!")
                      },
            oneTimeClick = true
        )

        ChatUtils.chat("Saved all egg locations.")
    }

    fun collectedEggsThisIsland() = currentIslandCollectedEggs()?.size ?: 0

    fun hasCollectedEgg(location: LorenzVec) =
        currentIslandCollectedEggs()?.contains(location) ?: false

}
