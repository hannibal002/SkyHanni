package at.hannibal2.hanni.features.mining.glacitemineshaft

import at.hannibal2.hanni.utils.LorenzVec

data class MineshaftWaypoint(
    val waypointType: MineshaftWaypointType,
    val location: LorenzVec,
    var shared: Boolean = false,
    var isCorpse: Boolean = false
)
