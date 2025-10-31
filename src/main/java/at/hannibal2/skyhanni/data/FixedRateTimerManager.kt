package at.hannibal2.hanni.data

import at.hannibal2.hanni.events.SecondPassedEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.DelayedRun
import at.hannibal2.hanni.utils.SkyBlockUtils
import kotlin.concurrent.fixedRateTimer

@HanniModule
object FixedRateTimerManager {
    private var totalSeconds = 0

    init {
        fixedRateTimer(name = "hanni-fixed-rate-timer-manager", period = 1000L) {
            DelayedRun.onThread.execute {
                if (!SkyBlockUtils.onHypixel) return@execute
                SecondPassedEvent(totalSeconds).post()
                totalSeconds++
            }
        }
    }
}
