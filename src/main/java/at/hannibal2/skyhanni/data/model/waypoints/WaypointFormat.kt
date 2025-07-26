package at.hannibal2.skyhanni.data.model.waypoints

interface WaypointFormat {
    fun deserialize(string: String): WaypointSet<SkyhanniWaypoint>?
    fun canSerialize(string: String): Boolean
    fun serialize(waypoints: WaypointSet<SkyhanniWaypoint>): String
    val name: String
}
