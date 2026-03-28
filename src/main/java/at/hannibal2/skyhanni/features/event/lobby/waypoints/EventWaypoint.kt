package at.hannibal2.skyhanni.features.event.lobby.waypoints

import at.hannibal2.skyhanni.data.jsonobjects.repo.EventWaypointData
import net.minecraft.world.phys.Vec3

data class EventWaypoint(
    val name: String = "",
    val position: Vec3,
) {
    var isFound: Boolean = false
}

fun loadEventWaypoints(
    waypoints: Map<String, List<EventWaypointData>>,
): Map<String, MutableSet<EventWaypoint>> = buildMap {
    waypoints.forEach { (name, position) ->
        put(
            name,
            buildSet {
                position.forEach { waypoint -> add(EventWaypoint(waypoint.name, waypoint.position)) }
            }.toMutableSet(),
        )
    }
}
