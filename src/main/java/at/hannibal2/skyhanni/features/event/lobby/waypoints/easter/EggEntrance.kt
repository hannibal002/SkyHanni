package at.hannibal2.skyhanni.features.event.lobby.waypoints.easter

import at.hannibal2.skyhanni.utils.LorenzVec

enum class EggEntrance(
    val eggEntranceName: String,
    val waypoint: LorenzVec,
    vararg val easterEgg: EasterEgg,
) {
    // not needed for this years eggs, also we have the path helper from the "only show next egg" option showing a full path to the next one
}

