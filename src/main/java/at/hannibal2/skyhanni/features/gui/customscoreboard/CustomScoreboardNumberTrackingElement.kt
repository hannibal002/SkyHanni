package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.formatNumber
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

interface CustomScoreboardNumberTrackingElement {
    var previousAmount: Long
    val numberColor: String
    var temporaryChangeDisplay: String?
    var currentJob: Job?

    fun checkDifference(currentAmount: Long) {
        if (!SkyHanniMod.feature.gui.customScoreboard.display.showNumberDifference) return
        if (currentAmount != previousAmount) {
            val changeAmount = currentAmount - previousAmount
            showTemporaryChange(changeAmount)
            previousAmount = currentAmount
        }
    }

    private fun showTemporaryChange(changeAmount: Long, durationMillis: Long = 5000) {
        currentJob?.cancel()
        temporaryChangeDisplay = if (changeAmount > 0) {
            " §7($numberColor+${formatNumber(changeAmount)}§7)$numberColor"
        } else {
            " §7($numberColor${formatNumber(changeAmount)}§7)$numberColor"
        }

        currentJob = SkyHanniMod.coroutineScope.launch {
            delay(durationMillis)
            temporaryChangeDisplay = null
            currentJob = null
        }
    }
}

