package at.hannibal2.skyhanni.features.mining.glacitemineshaft

import at.hannibal2.skyhanni.utils.LorenzColor

enum class MineshaftWaypointType(
    val display: String,
    val color: LorenzColor,
) {
    LAPIS("Lapis Corpse", LorenzColor.DARK_BLUE),
    UMBER("Umber Corpse", LorenzColor.GOLD),
    TUNGSTEN("Tungsten Corpse", LorenzColor.GRAY),
    VANGUARD("Vanguard Corpse", LorenzColor.BLUE),
    ENTRANCE("Entrance", LorenzColor.YELLOW),
    LADDER("Ladder", LorenzColor.YELLOW)
}
