package at.hannibal2.skyhanni.data.jsonobjects.repo

import at.hannibal2.skyhanni.utils.LorenzVec
import com.google.gson.annotations.Expose

data class EventWaypointsJson(
    @Expose val presents: Map<String, List<EventWaypointData>>,
    @Expose val baskets: Map<String, List<EventWaypointData>>,
)

data class EventWaypointData(
    @Expose val position: LorenzVec,
)
