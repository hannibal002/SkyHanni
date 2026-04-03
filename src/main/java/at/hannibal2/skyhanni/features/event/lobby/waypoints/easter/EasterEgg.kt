package at.hannibal2.skyhanni.features.event.lobby.waypoints.easter

import at.hannibal2.skyhanni.utils.LorenzVec

enum class EasterEgg(val eggName: String, val waypoint: LorenzVec) {
    EASTER_EGG_1("#1", LorenzVec(-45, 93, 3)),
    EASTER_EGG_2("#2", LorenzVec(-27, 80, 8)),
    EASTER_EGG_3("#3", LorenzVec(-12, 65, 3)),
    EASTER_EGG_4("#4", LorenzVec(20, 68, -30)),
    EASTER_EGG_5("#5", LorenzVec(19, 58, -45)),
    EASTER_EGG_6("#6", LorenzVec(35, 64, 20)),
    EASTER_EGG_7("#7", LorenzVec(85, 67, -27)),
    EASTER_EGG_8("#8", LorenzVec(91, 63, -117)),
    EASTER_EGG_9("#9", LorenzVec(129, 71, -141)),
    EASTER_EGG_10("#10", LorenzVec(69, 60, -165)),
    EASTER_EGG_11("#11", LorenzVec(67, 105, -137)),
    EASTER_EGG_12("#12", LorenzVec(-5, 61, -182)),
    EASTER_EGG_13("#13", LorenzVec(-123, 55, -182)),
    EASTER_EGG_14("#14", LorenzVec(-127, 70, -180)),
    EASTER_EGG_15("#15", LorenzVec(-158, 66, -130)),
    EASTER_EGG_16("#16", LorenzVec(-179, 82, -16)),
    EASTER_EGG_17("#17", LorenzVec(-140, 100, 0)),
    EASTER_EGG_18("#18", LorenzVec(-48, 87, 30)),
    EASTER_EGG_19("#19", LorenzVec(-43, 87, 39)),
    EASTER_EGG_20("#20", LorenzVec(-15, 65, 109)),
    EASTER_EGG_21("#21", LorenzVec(34, 59, 118)),
    EASTER_EGG_22("#22", LorenzVec(49, 65, 91)),
    EASTER_EGG_23("#23", LorenzVec(96, 69, 120)),
    EASTER_EGG_24("#24", LorenzVec(162, 60, 130)),
    EASTER_EGG_25("#25", LorenzVec(177, 96, -6)),
    EASTER_EGG_26("#26", LorenzVec(188, 57, -46)),
    EASTER_EGG_27("#27", LorenzVec(-47, 65, 152)),
    EASTER_EGG_28("#28", LorenzVec(-71, 61, 203)),
    EASTER_EGG_29("#29", LorenzVec(-68, 54, 243)),
    EASTER_EGG_30("#30", LorenzVec(55, 64, 251)),
    ;

    var found = false
}
