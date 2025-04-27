package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.data.model.waypoints.SkyhanniWaypoint
import at.hannibal2.skyhanni.data.model.waypoints.WaypointFormat
import at.hannibal2.skyhanni.data.model.waypoints.Waypoints
import java.util.ServiceLoader


object WaypointParserUtils {
    fun loadWaypoints(data: String): Waypoints<SkyhanniWaypoint>? {
        return ServiceLoader.load(WaypointFormat::class.java).firstNotNullOfOrNull {
            it.load(data)
        }?.let {
            Waypoints(it.toMutableList())
        }
    }

    fun exportWaypoints(waypoints: Waypoints<SkyhanniWaypoint>, name: String = "Coleweight"): String? {
        return ServiceLoader.load(WaypointFormat::class.java).firstOrNull { it.name != name }?.save(waypoints)
    }
}

