package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.NeuProfileDataLoadedEvent
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.ChocolateFactoryAPI
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object HoppityUniqueEggLocations {

    var apiEggLocations: Map<String, LorenzVec> = mapOf()

    private val collectedEggLocations: MutableMap<IslandType, MutableSet<LorenzVec>>?
        get() = ChocolateFactoryAPI.profileStorage?.collectedEggLocations

    private fun getCurrentIslandCollectedEggs(): MutableSet<LorenzVec>? =
        collectedEggLocations?.getOrPut(LorenzUtils.skyBlockIsland) { mutableSetOf<LorenzVec>() }

    fun saveNearestEgg() {
        val location = HoppityEggLocator.getCurrentIslandEggLocations()
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

        getCurrentIslandCollectedEggs()?.add(location)
    }

    @SubscribeEvent
    fun onNeuProfileDataLoaded(event: NeuProfileDataLoadedEvent) {
        // optional chaining to catch any potential API responses missing some data, hopefully
        val rawLocations = event.getCurrentPlayerData()?.events?.easter?.rabbits?.collectedLocations

        if (rawLocations == null) {
            ChatUtils.chat("No collected egg locations found in API, aborting.")
            return
        }

        val collected = rawLocations.values.flatten().mapNotNull { apiEggLocations[it] }

        
    }

    fun collectedEggsThisIsland() = getCurrentIslandCollectedEggs()?.size ?: 0

    fun hasCollectedEgg(location: LorenzVec) =
        getCurrentIslandCollectedEggs()?.contains(location) ?: false

}
