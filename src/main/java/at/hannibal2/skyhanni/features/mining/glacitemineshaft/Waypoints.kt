package at.hannibal2.skyhanni.features.mining.glacitemineshaft

import at.hannibal2.skyhanni.utils.LorenzVec

data class Waypoint(val waypointType: WaypointsType, val location: LorenzVec, var shared: Boolean = false)
