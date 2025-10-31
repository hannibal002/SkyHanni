package at.hannibal2.hanni.data.model.waypoints

interface WaypointFormat {
    fun load(string: String): Waypoints<HanniWaypoint>?
    fun canLoad(string: String): Boolean
    fun export(waypoints: Waypoints<HanniWaypoint>): String
    val name: String
}
