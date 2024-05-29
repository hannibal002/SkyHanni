package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.ChocolateFactoryAPI
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object HoppityUniqueEggLocations {

    private val islandEggLocations: List<LorenzVec>?
        get() = HoppityEggLocator.getCurrentIslandEggLocations()

    private val collectedEggLocations
        get() = ChocolateFactoryAPI.profileStorage?.collectedEggLocations

    private fun getCurrentIslandCollectedEggs(): MutableSet<LorenzVec>? {
        var currentSet = collectedEggLocations?.get(LorenzUtils.skyBlockIsland)
        if (currentSet == null) {
            currentSet = mutableSetOf<LorenzVec>()
            collectedEggLocations?.set(LorenzUtils.skyBlockIsland, currentSet)
        }
        return currentSet
    }

    fun saveNearestEgg() {
        val location = islandEggLocations?.minByOrNull { it.distanceSqToPlayer() } ?: return
        if (location.distanceSqToPlayer() > 100) {
            ChatUtils.debug("""
                Player too far from any known egg location! 
                Player location: ${LocationUtils.playerLocation()}
                Closest egg: $location
            """.trimIndent())
            return
        }

        val collectedEggs = getCurrentIslandCollectedEggs() ?: return
        collectedEggs.add(location)
    }

    fun hasCollectedEgg(location: LorenzVec) =
        getCurrentIslandCollectedEggs()?.contains(location) ?: false

}
