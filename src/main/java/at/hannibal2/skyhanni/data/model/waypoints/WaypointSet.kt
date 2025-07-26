package at.hannibal2.skyhanni.data.model.waypoints

import com.google.gson.annotations.Expose

open class WaypointSet<T : AbstractWaypoint<T>>(
    @Expose open val waypoints: MutableList<T> = mutableListOf(),
) : MutableList<T> by waypoints {
    open fun deepCopy() = WaypointSet(waypoints.toMutableList())
}
