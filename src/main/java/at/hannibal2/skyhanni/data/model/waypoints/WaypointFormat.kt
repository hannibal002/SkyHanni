package at.hannibal2.skyhanni.data.model.waypoints

interface WaypointFormat {
    fun load(string: String): Waypoints<SkyHanniWaypoint>?
    fun canLoad(string: String): Boolean
    fun export(waypoints: Waypoints<SkyHanniWaypoint>): String
    val name: String
}
