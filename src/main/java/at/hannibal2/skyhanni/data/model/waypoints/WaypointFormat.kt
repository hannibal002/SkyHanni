package at.hannibal2.skyhanni.data.model.waypoints

interface WaypointFormat {
    fun load(string: String): Waypoints<SkyhanniWaypoint>?
    fun canLoad(string: String): Boolean
    fun export(waypoints: Waypoints<SkyhanniWaypoint>): String
    val name: String
}
