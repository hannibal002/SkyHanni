package at.hannibal2.hanni.data

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.jsonobjects.repo.LocationFixJson
import at.hannibal2.hanni.events.RepositoryReloadEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.LocationUtils.isPlayerInside
import net.minecraft.util.AxisAlignedBB

@HanniModule
object LocationFixData {

    private val locationFixes = mutableMapOf<IslandType, List<LocationFix>>()

    private data class LocationFix(val area: AxisAlignedBB, val realLocation: String)

    // priority set to low so that IslandType can load their island names from repo earlier
    @HandleEvent(priority = HandleEvent.LOW)
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<LocationFixJson>("LocationFix")
        locationFixes.clear()

        for (fix in data.locationFixes.values) {
            val island = IslandType.getByName(fix.islandName)
            val area = fix.a.axisAlignedTo(fix.b)
            val realLocation = fix.realLocation

            val list = locationFixes[island]

            val locationFix = LocationFix(area, realLocation)

            if (list == null) locationFixes[island] = listOf(locationFix)
            else locationFixes[island] = list + locationFix
        }
    }

    fun fixLocation(skyBlockIsland: IslandType): String? =
        locationFixes[skyBlockIsland]
            ?.find { it.area.isPlayerInside() }
            ?.realLocation

}
