package at.hannibal2.skyhanni.data.model.waypoints

interface WaypointFormat {
    fun load(string: String): Collection<SkyhanniWaypoint>?
    fun canLoad(string: String): Boolean
    fun save(waypoints: Collection<SkyhanniWaypoint>): String
    val name: String
}
