package at.hannibal2.skyhanni.features.mining.glacitemineshaft

import net.minecraft.world.phys.Vec3

data class MineshaftWaypoint(
    val waypointType: MineshaftWaypointType,
    val location: Vec3,
) {
    val isCorpse: Boolean = waypointType.helmet != null

    var shared: Boolean = false
}
