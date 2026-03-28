package at.hannibal2.skyhanni.features.event.lobby.waypoints.easter

import net.minecraft.world.phys.Vec3

// TODO use repo
enum class EggEntrance(
    val eggEntranceName: String,
    val waypoint: Vec3,
    vararg val easterEgg: EasterEgg,
) {

    EASTER_EGG_ENTER_8(
        "#8 (go down here)",
        Vec3(94.0, 78.0, 44.0),
        EasterEgg.EASTER_EGG_8
    ),
    EASTER_EGG_ENTER_21(
        "#21 (enter cave)",
        Vec3(-55.0, 86.0, -40.0),
        EasterEgg.EASTER_EGG_21
    ),
    EASTER_EGG_ENTER_22(
        "#22 (enter here)",
        Vec3(-97.0, 111.0, 22.0),
        EasterEgg.EASTER_EGG_22
    )
}

