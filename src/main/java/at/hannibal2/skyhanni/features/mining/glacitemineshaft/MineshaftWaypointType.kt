package at.hannibal2.skyhanni.features.mining.glacitemineshaft

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.LorenzColor

private val config get() = SkyHanniMod.feature.mining.glaciteMineshaft.waypointsConfig

enum class MineshaftWaypointType(
    val display: String,
    val color: LorenzColor,
    val isCorpse: Boolean,
    val renderCondition: () -> Boolean,
) {
    LAPIS("Lapis Corpse", LorenzColor.DARK_BLUE, true, { config.types.foundCorpse }),
    UMBER("Umber Corpse", LorenzColor.GOLD, true, { config.types.foundCorpse }),
    TUNGSTEN("Tungsten Corpse", LorenzColor.GRAY, true, { config.types.foundCorpse }),
    VANGUARD("Vanguard Corpse", LorenzColor.BLUE, true, { config.types.foundCorpse }),
    ENTRANCE("Entrance", LorenzColor.YELLOW, false, { config.types.entrance }),
    LADDER("Ladder", LorenzColor.YELLOW, false, { config.types.ladder }),
}
