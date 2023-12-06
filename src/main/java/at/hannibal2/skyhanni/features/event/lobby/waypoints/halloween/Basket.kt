package at.hannibal2.skyhanni.features.event.lobby.waypoints.halloween

import at.hannibal2.skyhanni.utils.LorenzVec

enum class Basket(val basketName: String, val waypoint: LorenzVec) {
    BASKET_1("#1", LorenzVec(-31, 91, -19)),
    BASKET_2("#2", LorenzVec(-14, 85, -78)),
    BASKET_3("#3", LorenzVec(24, 68, -29)),
    BASKET_4("#4", LorenzVec(57, 64, -90)),
    BASKET_5("#5", LorenzVec(129, 70, -130)),
    BASKET_6("#6", LorenzVec(-5, 62, -181)),
    BASKET_7("#7", LorenzVec(-145, 74, -137)),
    BASKET_8("#8", LorenzVec(-7, 65, 130)),
    BASKET_9("#9", LorenzVec(-7, 57, 196)),
    BASKET_10("#10", LorenzVec(-75, 61, 195)),
    BASKET_11("#11", LorenzVec(-131, 61, 152)),
    BASKET_12("#12", LorenzVec(-107, 24, 27)),
    BASKET_13("#13", LorenzVec(-6, 63, 23)),
    BASKET_14("#14", LorenzVec(29, 71, 24)),
    BASKET_15("#15", LorenzVec(136, 64, -10)),
    BASKET_16("#16", LorenzVec(199, 85, -41)),
    BASKET_17("#17", LorenzVec(199, 58, -41)),
    BASKET_18("#18", LorenzVec(112, 45, -15)),
    BASKET_19("#19", LorenzVec(111, 65, 31)),
    BASKET_20("#20", LorenzVec(138, 62, 94)),
    BASKET_21("#21", LorenzVec(109, 67, 149)),
    BASKET_22("#22", LorenzVec(94, 53, 238)),
    BASKET_23("#23", LorenzVec(-84, 72, 8)),
    BASKET_24("#24", LorenzVec(-13, 31, -26)),
    BASKET_25("#25 (get your code first!)", LorenzVec(-32, 14, 102)),
    ;

    var found = false
}
