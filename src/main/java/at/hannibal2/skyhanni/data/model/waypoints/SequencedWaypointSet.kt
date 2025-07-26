package at.hannibal2.skyhanni.data.model.waypoints

import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.system.LazyVar
import com.google.gson.annotations.Expose

class SequencedWaypointSet<T : AbstractSequencedWaypoint>(
    @Expose override val waypoints: MutableList<T> = mutableListOf(),
    @Expose var currentIndex: Int = 0,
) : WaypointSet<T>() {
    private var waypointsHash: Int = 0
    private var orderedWaypointsCache: MutableMap<Int, T> by LazyVar { mutableMapOf() }
    val sequencedWaypoints get() = orderedWaypointsCache.takeIf { waypointsHash == waypoints.hashCode()}
        ?: waypoints.associateBy { it.number }.toMutableMap().also {
            orderedWaypointsCache = it
        }

    override fun deepCopy(): SequencedWaypointSet<T> = SequencedWaypointSet(waypoints, currentIndex)

    fun incrementIndex(increment: Int) {
        currentIndex = Math.floorMod(currentIndex + increment, this.size)
    }

    fun addNumbered(element: T, showFeedback: Boolean = true) {
        if (element.number == this.size + 1) this.add(element)
        else add(element.number - 1, element)
        if (showFeedback) ChatUtils.chat("Inserted waypoint $element.number at ${element.location.toCleanString()}.")
    }

    override fun add(element: T): Boolean = super.add(element).also {
        waypointsHash = waypoints.hashCode()
    }

    override fun add(index: Int, element: T) {
        shiftRight(index)
        super.add(index, element).also {
            waypointsHash = waypoints.hashCode()
        }
    }

    override fun removeAt(index: Int): T {
        shiftLeft(index)
        return super.removeAt(index).also {
            waypointsHash = waypoints.hashCode()
        }
    }

    /**
     * Shifts all elements in the sequence "up" one number (to the right, in the logical list),
     * to make space for a new waypoint at [startIndex].
     * @param startIndex The index at which to insert the new waypoint, inclusive.
     */
    private fun shiftRight(startIndex: Int) = shiftAll(startIndex, 1)

    /**
     * Shifts all elements in the sequence "down" one number (to the left, in the logical list),
     * to clean up after removing a waypoint at [startIndex].
     * @param startIndex The index at which the waypoint was removed, inclusive.
     */
    private fun shiftLeft(startIndex: Int) = shiftAll(startIndex, -1)

    private fun shiftAll(
        startIndex: Int,
        scalar: Int,
    ) = waypoints.filterIndexed { index, _ ->
        index >= startIndex
    }.forEach { waypoint ->
        waypoint.number += scalar
        if (waypoint !is AbstractDescriptiveWaypoint || !waypoint.hasCustomName()) {
            (waypoint as AbstractNamedWaypoint).name = waypoint.number.toString()
        }
    }.also { waypointsHash = waypoints.hashCode() }

    private fun setWaypoint(index: Int, block: (T) -> Unit) {
        if (index < 0 || index >= waypoints.size) return
        val waypoint = waypoints[index]
        block(waypoint)
        orderedWaypointsCache[waypoint.number] = waypoint
        waypointsHash = waypoints.hashCode()
    }
}
