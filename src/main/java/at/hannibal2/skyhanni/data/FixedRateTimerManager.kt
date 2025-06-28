package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import kotlin.concurrent.fixedRateTimer

@SkyHanniModule
object FixedRateTimerManager {
    private var totalSeconds = 0

    init {
        fixedRateTimer(name = "skyhanni-fixed-rate-timer-manager", period = 1000L) {
            DelayedRun.onThread.execute {
                if (!SkyBlockUtils.onHypixel) return@execute
                SecondPassedEvent(totalSeconds).post()
                totalSeconds++
            }
        }
    }
}
