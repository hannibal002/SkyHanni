package at.hannibal2.hanni.config.storage

import at.hannibal2.hanni.data.model.waypoints.HanniWaypoint
import at.hannibal2.hanni.data.model.waypoints.Waypoints
import com.google.gson.annotations.Expose

class OrderedWaypointsRoutes {
    @Expose
    var routes: MutableMap<String, Waypoints<HanniWaypoint>>? = null
}
