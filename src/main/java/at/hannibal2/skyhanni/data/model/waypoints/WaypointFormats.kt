package at.hannibal2.skyhanni.data.model.waypoints

import java.util.ServiceLoader

object WaypointFormats {

    // TODO add Skyblocker waypoint format
    fun load(data: String): Pair<Waypoints<SkyHanniWaypoint>, String>? =
        ServiceLoader.load(WaypointFormat::class.java).firstNotNullOfOrNull { format ->
            format.load(data)?.let { it to format.name }
        }

    fun export(waypoints: Waypoints<SkyHanniWaypoint>, name: String): String? =
        ServiceLoader.load(WaypointFormat::class.java).firstOrNull { it.name == name }?.export(waypoints)

    fun names(): List<String> = ServiceLoader.load(WaypointFormat::class.java).map { it.name }
}
