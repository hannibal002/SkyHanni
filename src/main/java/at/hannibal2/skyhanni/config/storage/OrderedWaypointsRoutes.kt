package at.hannibal2.skyhanni.config.storage

import at.hannibal2.skyhanni.data.model.waypoints.SkyhanniWaypoint
import at.hannibal2.skyhanni.data.model.waypoints.WaypointSet
import com.google.gson.annotations.Expose

class OrderedWaypointsRoutes {
    @Expose
    var routes: MutableMap<String, WaypointSet<SkyhanniWaypoint>> = mutableMapOf()
}
