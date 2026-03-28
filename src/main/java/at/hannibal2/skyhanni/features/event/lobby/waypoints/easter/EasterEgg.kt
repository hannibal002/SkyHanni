package at.hannibal2.skyhanni.features.event.lobby.waypoints.easter

import net.minecraft.world.phys.Vec3

// TODO use repo
enum class EasterEgg(val eggName: String, val waypoint: Vec3) {
    EASTER_EGG_1("#1", Vec3(-34.0, 92.0, -20.0)),
    EASTER_EGG_2("#2", Vec3(-28.0, 85.0, -46.0)),
    EASTER_EGG_3("#3", Vec3(10.0, 69.0, -19.0)),
    EASTER_EGG_4("#4", Vec3(41.0, 67.0, -31.0)),
    EASTER_EGG_5("#5", Vec3(5.0, 69.0, 21.0)),
    EASTER_EGG_6("#6", Vec3(56.0, 71.0, 67.0)),
    EASTER_EGG_7("#7", Vec3(103.0, 75.0, 43.0)),
    EASTER_EGG_8("#8", Vec3(121.0, 64.0, 29.0)), // entrance 94, 78, 44
    EASTER_EGG_9("#9", Vec3(146.0, 77.0, 42.0)),
    EASTER_EGG_10("#10", Vec3(126.0, 68.0, -19.0)),
    EASTER_EGG_11("#11", Vec3(167.0, 79.0, -10.0)),
    EASTER_EGG_12("#12", Vec3(179.0, 68.0, -12.0)),
    EASTER_EGG_13("#13", Vec3(192.0, 75.0, -34.0)),
    EASTER_EGG_14("#14", Vec3(149.0, 68.0, -115.0)),
    EASTER_EGG_15("#15", Vec3(74.0, 60.0, -176.0)),
    EASTER_EGG_16("#16", Vec3(-1.0, 67.0, -146.0)),
    EASTER_EGG_17("#17", Vec3(-117.0, 71.0, -143.0)),
    EASTER_EGG_18("#18", Vec3(-166.0, 58.0, -96.0)),
    EASTER_EGG_19("#19", Vec3(-188.0, 61.0, -56.0)),
    EASTER_EGG_20("#20", Vec3(-118.0, 99.0, -48.0)),
    EASTER_EGG_21("#21", Vec3(-88.0, 74.0, -26.0)), // entrance -55, 86, -40
    EASTER_EGG_22("#22", Vec3(-84.0, 108.0, 13.0)), // entrance -97, 111, 22
    EASTER_EGG_23("#23", Vec3(-131.0, 85.0, -2.0)),
    EASTER_EGG_24("#24", Vec3(-179.0, 62.0, 45.0)),
    EASTER_EGG_25("#25", Vec3(-163.0, 56.0, 141.0)),
    EASTER_EGG_26("#26", Vec3(-61.0, 72.0, 125.0)),
    EASTER_EGG_27("#27", Vec3(-53.0, 90.0, 89.0)),
    EASTER_EGG_28("#28", Vec3(-2.0, 67.0, 143.0)),
    EASTER_EGG_29("#29", Vec3(68.0, 74.0, 145.0)),
    EASTER_EGG_30("#30", Vec3(134.0, 67.0, 131.0)),
    ;

    var found = false
}
