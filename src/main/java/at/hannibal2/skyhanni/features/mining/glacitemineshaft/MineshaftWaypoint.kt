package at.hannibal2.skyhanni.features.mining.glacitemineshaft

import at.hannibal2.skyhanni.utils.LorenzVec

data class MineshaftWaypoint(
    var waypointType: MineshaftWaypointType,
    val location: LorenzVec,
    var shared: Boolean = false,
    var isCorpse: Boolean = false,
    var isLootedCorpse: Boolean = false,
) {
    val colorCode get() = when {
        isLootedCorpse -> "§a"
        waypointType == MineshaftWaypointType.POTENTIAL -> "§b"
        else -> "§e"
    }

    val scale get() = if (waypointType == MineshaftWaypointType.POTENTIAL) 0.6 else 1.0
}
