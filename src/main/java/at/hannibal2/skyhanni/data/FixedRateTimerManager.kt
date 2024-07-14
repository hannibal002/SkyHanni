package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import kotlin.concurrent.fixedRateTimer

@SkyHanniModule
object FixedRateTimerManager {
    private var totalSeconds = 0

    init {
        fixedRateTimer(name = "skyhanni-fixed-rate-timer-manager", period = 1000L) {
            Minecraft.getMinecraft().addScheduledTask {
                if (!LorenzUtils.onHypixel) return@addScheduledTask
                SecondPassedEvent(totalSeconds).postAndCatch()
                totalSeconds++
            }
        }
    }
}
