package at.hannibal2.skyhanni.features.misc.halloweenlobbywaypoints

import at.hannibal2.skyhanni.utils.LorenzVec

enum class BasketEntrances(
    val basketEntranceName: String,
    val waypoint: LorenzVec
) {
    BASKET_ENTER_23("#23, #24 (behind the lava)", LorenzVec(-138, 74, -4)),
    BASKET_ENTER_24("#24 (within this tunnel)", LorenzVec(-80, 72, -4)),
    BASKET_ENTER_25_1("#25 (1st digit, SNEAK + RCLICK)", LorenzVec(143, 65, -30)),
    BASKET_ENTER_25_2("#25 (3rd digit, open chest)", LorenzVec(205, 34, -157)),
    BASKET_ENTER_25_3("#25 (inside this well)", LorenzVec(10, 63, 0)),
    BASKET_ENTER_25_4("#25 (left turn [<--] here)", LorenzVec(-28, 41, 14)),
    BASKET_ENTER_25_5("#25 Vault (brute force 2nd digit)", LorenzVec(-35, 25, 63)),
}
