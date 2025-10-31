package at.hannibal2.hanni.features.gui.customscoreboard

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.features.gui.customscoreboard.CustomScoreboardUtils.formatNumber
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

interface CustomScoreboardNumberTrackingElement {
    var previousAmount: Long
    val numberColor: String
    var temporaryChangeDisplay: String?
    var currentJob: Job?

    fun checkDifference(currentAmount: Long) {
        if (!HanniMod.feature.gui.customScoreboard.display.showNumberDifference) return
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
        currentJob = HanniMod.launchCoroutine("custom scoreboard number show temporary change") {
            delay(durationMillis)
            temporaryChangeDisplay = null
            currentJob = null
        }
    }
}

