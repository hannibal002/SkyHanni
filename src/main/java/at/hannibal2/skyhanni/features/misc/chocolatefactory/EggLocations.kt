package at.hannibal2.skyhanni.features.misc.chocolatefactory

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.jsonobjects.repo.HoppityEggLocation

object EggLocations {

    private var eggLocations: Map<IslandType, HoppityEggLocation> = mapOf()

//     @SubscribeEvent
//     fun onRepoReload(event: RepositoryReloadEvent) {
//         val data = event.getConstant<HoppityEggLocationsJson>("HoppityEggLocations")
//     }

}
