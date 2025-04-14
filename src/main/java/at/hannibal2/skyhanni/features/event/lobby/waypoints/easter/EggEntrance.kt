package at.hannibal2.skyhanni.features.event.lobby.waypoints.easter

import at.hannibal2.skyhanni.utils.LorenzVec

enum class EggEntrance(
    val eggEntranceName: String,
    val waypoint: LorenzVec,
    vararg val easterEgg: EasterEgg,
) {

    EASTER_EGG_ENTER_8(
        "#8 (go down here)",
        LorenzVec(94, 78, 44),
        EasterEgg.EASTER_EGG_8
    ),
    EASTER_EGG_ENTER_21(
        "#21 (enter cave)",
        LorenzVec(-55, 86, -40),
        EasterEgg.EASTER_EGG_21
    ),
    EASTER_EGG_ENTER_22(
        "#22 (enter here)",
        LorenzVec(-97, 111, 22),
        EasterEgg.EASTER_EGG_22
    )
}

