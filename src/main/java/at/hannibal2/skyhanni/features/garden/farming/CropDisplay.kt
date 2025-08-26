package at.hannibal2.skyhanni.features.garden.farming

import EliteLeaderboardDisplay
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardMode
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.features.garden.CropType

class CropDisplay(
    storage: Pair<CropType?, EliteLeaderboardMode>?,
) : EliteLeaderboardDisplay<CropType, EliteLeaderboardType.Crop>(
    storage,
    { crop, mode -> EliteLeaderboardType.Crop(crop, mode) } // explicit factory
) {
    override fun render() {
        val type = currentLeaderboardType ?: return
        println("Showing crop leaderboard for ${type.enumValue} in ${type.mode}")
        println("Leaderboard key = ${type.lbName}")
    }
}
