package at.hannibal2.skyhanni.features.mining.glacitemineshaft

import at.hannibal2.skyhanni.utils.LorenzColor

enum class WaypointsType(val displayText: String, val helmetName: String, val color: LorenzColor) {
    LAPIS("Lapis Corpse", "Lapis Armor Helmet", LorenzColor.DARK_BLUE),
    UMBER("Umber Corpse", "Yog Helmet", LorenzColor.GOLD),
    TUNGSTEN("Tungsten Corpse", "Mineral Helmet", LorenzColor.GRAY),
    VANGUARD("Vanguard Corpse", "Vanguard Helmet", LorenzColor.BLUE),
    ENTRANCE("Entrance", "", LorenzColor.YELLOW),
    LADDER("Ladder", "", LorenzColor.YELLOW),
    UNKNOWN("Unknown", "", LorenzColor.BLACK)
}
