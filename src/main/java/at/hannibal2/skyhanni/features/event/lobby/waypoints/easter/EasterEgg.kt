package at.hannibal2.skyhanni.features.event.lobby.waypoints.easter

import at.hannibal2.skyhanni.utils.LorenzVec

enum class EasterEgg(val eggName: String, val waypoint: LorenzVec) {
    EASTER_EGG_1("#1", LorenzVec(-34, 92, -20)),
    EASTER_EGG_2("#2", LorenzVec(-28, 85, -46)),
    EASTER_EGG_3("#3", LorenzVec(10, 69, -19)),
    EASTER_EGG_4("#4", LorenzVec(41, 67, -31)),
    EASTER_EGG_5("#5", LorenzVec(5, 69, 21)),
    EASTER_EGG_6("#6", LorenzVec(56, 71, 67)),
    EASTER_EGG_7("#7", LorenzVec(103, 75, 43)),
    EASTER_EGG_8("#8", LorenzVec(121, 64, 29)), // entrance 94, 78, 44
    EASTER_EGG_9("#9", LorenzVec(146, 77, 42)),
    EASTER_EGG_10("#10", LorenzVec(126, 68, -19)),
    EASTER_EGG_11("#11", LorenzVec(167, 79, -10)),
    EASTER_EGG_12("#12", LorenzVec(179, 68, -12)),
    EASTER_EGG_13("#13", LorenzVec(192, 75, -34)),
    EASTER_EGG_14("#14", LorenzVec(149, 68, -115)),
    EASTER_EGG_15("#15", LorenzVec(74, 60, -176)),
    EASTER_EGG_16("#16", LorenzVec(-1, 67, -146)),
    EASTER_EGG_17("#17", LorenzVec(-117, 71, -143)),
    EASTER_EGG_18("#18", LorenzVec(-166, 58, -96)),
    EASTER_EGG_19("#19", LorenzVec(-188, 61, -56)),
    EASTER_EGG_20("#20", LorenzVec(-118, 99, -48)),
    EASTER_EGG_21("#21", LorenzVec(-88, 74, -26)), // entrance -55, 86, -40
    EASTER_EGG_22("#22", LorenzVec(-84, 108, 13)), // entrance -97, 111, 22
    EASTER_EGG_23("#23", LorenzVec(-131, 85, -2)),
    EASTER_EGG_24("#24", LorenzVec(-179, 62, 45)),
    EASTER_EGG_25("#25", LorenzVec(-163, 56, 141)),
    EASTER_EGG_26("#26", LorenzVec(-61, 72, 125)),
    EASTER_EGG_27("#27", LorenzVec(-53, 90, 89)),
    EASTER_EGG_28("#28", LorenzVec(-2, 67, 143)),
    EASTER_EGG_29("#29", LorenzVec(68, 74, 145)),
    EASTER_EGG_30("#30", LorenzVec(134, 67, 131)),
    ;

    var found = false
}
