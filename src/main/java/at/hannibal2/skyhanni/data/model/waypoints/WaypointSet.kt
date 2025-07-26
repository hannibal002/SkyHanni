package at.hannibal2.skyhanni.data.model.waypoints

import at.hannibal2.skyhanni.utils.system.LazyVar
import com.google.gson.annotations.Expose

open class WaypointSet<T : AbstractWaypoint<T>>(
    @Expose open val waypoints: MutableList<T> = mutableListOf(),
) : MutableList<T> by waypoints {
    fun deepCopy() = transform { it.duplicate() }

    inline fun <R : AbstractWaypoint<R>> transform(transform: (T) -> R): WaypointSet<R> = WaypointSet(waypoints.map { transform(it) }.toMutableList())
}

class SequencedWaypointSet<T>(
    @Expose override val waypoints: MutableList<T> = mutableListOf(),
) : WaypointSet<T>() where T : AbstractWaypoint<T>, T : AbstractSequencedWaypoint {
    private var waypointsHash: Int by LazyVar { waypoints.hashCode() }
    private var orderedWaypointsCache: MutableMap<Int, T> by LazyVar { mutableMapOf() }
    val orderedWaypoints get() = if (waypointsHash == waypoints.hashCode()) orderedWaypointsCache else {
        waypoints.associateBy { it.number }.toMutableMap().also {
            orderedWaypointsCache = it
        }
    }
}
