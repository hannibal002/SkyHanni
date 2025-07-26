package at.hannibal2.skyhanni.config.storage

import at.hannibal2.skyhanni.data.model.waypoints.SequencedWaypointSet
import at.hannibal2.skyhanni.data.model.waypoints.SkyhanniWaypoint
import com.google.gson.annotations.Expose

class OrderedWaypointsRoutes {
    @Expose
    var routes: MutableMap<String, SequencedWaypointSet<SkyhanniWaypoint>> = mutableMapOf()
}
