package at.hannibal2.skyhanni.data.model.waypoints

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.utils.json.fromJson

/**
 * @param N the native type of the waypoint, which extends [AbstractWaypoint].
 */
interface AbstractWaypointFormat<N> where N : AbstractWaypoint {
    val formatType: WaypointFormatType
    val name get() = formatType.name.lowercase()

    fun deserialize(string: String): WaypointSet<N>? = ConfigManager.gson.fromJson<WaypointSet<N>>(string)
    fun serialize(set: WaypointSet<N>): String = ConfigManager.gson.toJson(set)
}

enum class WaypointFormatType(val castClazz: Class<out AbstractWaypoint>) {
    SKYHANNI(SkyhanniWaypoint::class.java),
    COLEWEIGHT(ColeweightWaypoint::class.java),
    SKYTILS(SkytilsWaypoint::class.java),
}
