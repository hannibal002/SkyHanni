package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.getAmount
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.getLeaderboardPosition
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.getNextPlayer
import at.hannibal2.skyhanni.data.garden.FarmingWeight.getWeight
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardMode
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.farming.FarmingWeightDisplay.currentLeaderboardType
import at.hannibal2.skyhanni.features.garden.farming.FarmingWeightDisplay.drawDisplay
import at.hannibal2.skyhanni.features.garden.farming.FarmingWeightDisplay.leaderboardPos
import at.hannibal2.skyhanni.features.garden.farming.FarmingWeightDisplay.nextPlayer
import at.hannibal2.skyhanni.features.garden.farming.FarmingWeightDisplay.weight

open class EliteLeaderboardDisplay() {
    var amount: Double? = null
    var currentMode: EliteLeaderboardMode = EliteLeaderboardMode.ALL_TIME
    var currentType: CropType? = null
    var currentLeaderboardType: EliteLeaderboardType.Crop? = null

    fun createCurrentLeaderboardType() {
        currentType?.let {
            currentLeaderboardType = EliteLeaderboardType.Crop(it, currentMode)
        }
    }
    fun update(overrideCooldown: Boolean = false) {
        amount = currentLeaderboardType?.let { getAmount(it, overrideCooldown) }
        leaderboardPos = getLeaderboardPosition(currentLeaderboardType, overrideCooldown)
        nextPlayer = getNextPlayer(currentLeaderboardType)
        drawDisplay()
    }
}
