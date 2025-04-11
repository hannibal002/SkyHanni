package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.formatNumber
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

abstract class CustomScoreboardNumberTrackingElement {
    abstract var previousAmount: Long
    abstract val numberColor: String
    var temporaryChangeDisplay: String? = null
    var currentJob: Job? = null

    fun checkDifference(currentAmount: Long) {
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

