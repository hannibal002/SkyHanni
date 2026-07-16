package at.hannibal2.skyhanni.features.mining.glacitemineshaft

import at.hannibal2.skyhanni.utils.LorenzColor

enum class MineshaftWaypointType(
    val display: String,
    val color: LorenzColor,
    val scale: Double = 1.0,
    val maxAlpha: Float = 0.33f,
) {
    LAPIS("Lapis Corpse", LorenzColor.DARK_BLUE),
    UMBER("Umber Corpse", LorenzColor.GOLD),
    TUNGSTEN("Tungsten Corpse", LorenzColor.GRAY),
    VANGUARD("Vanguard Corpse", LorenzColor.BLUE),
    POTENTIAL("Potential Corpse", LorenzColor.WHITE, 0.6, 0.2f),
    ENTRANCE("Entrance", LorenzColor.YELLOW),
    LADDER("Ladder", LorenzColor.YELLOW)
}
