package at.hannibal2.skyhanni.features.event.lobby.waypoints.easter

import at.hannibal2.skyhanni.utils.LorenzVec

enum class Egg(val eggName: String, val waypoint: LorenzVec) {
    EGG_1("#1", LorenzVec(-47, 94, -3)),
    EGG_2("#2", LorenzVec(-20, 86, -70)),
    EGG_3("#3", LorenzVec(24, 62, -38)),
    EGG_4("#4", LorenzVec(-38, 56, 195)),
    EGG_5("#5", LorenzVec(-67, 82, 98)),
    EGG_6("#6", LorenzVec(-91, 61, 140)),
    EGG_7("#7", LorenzVec(103, 56, 194)),
    EGG_8("#8", LorenzVec(81, 68, 108)),
    EGG_9("#9", LorenzVec(10, 65, 58)),
    EGG_10("#10", LorenzVec(9, 53, 249)),
    EGG_11("#11", LorenzVec(216, 51, 93)),
    EGG_12("#12", LorenzVec(113, 45, 161)),
    EGG_13("#13", LorenzVec(133, 51, -8)),
    EGG_14("#14", LorenzVec(141, 73, 3)),
    EGG_15("#15", LorenzVec(107, 68, -9)),
    EGG_16("#16", LorenzVec(167, 60, -42)),
    EGG_17("#17", LorenzVec(58, 65, -2)),
    EGG_18("#18", LorenzVec(118, 51, -85)), // 158, 68, -81 entrance
    EGG_19("#19", LorenzVec(197, 60, 17)),
    EGG_20("#20", LorenzVec(-136, 85, -16)),
    EGG_21("#21", LorenzVec(-161, 57, -97)),
    EGG_22("#22", LorenzVec(-138, 74, -133)),
    EGG_23("#23", LorenzVec(-5, 77, -176)),
    EGG_24("#24", LorenzVec(67, 60, -170)),
    EGG_25("#25", LorenzVec(-177, 58, 70)),
    EGG_26("#26", LorenzVec(-141, 102, -15)),
    EGG_27("#27", LorenzVec(9, 32, 3)), // 11, 62, 0 entrance
    EGG_28("#28", LorenzVec(150, 28, 19)),
    EGG_29("#29", LorenzVec(47, 37, 52)),
    EGG_30("#30 (get your code first!)", LorenzVec(-28, 11, 123)),
    ;

    var found = false
}
