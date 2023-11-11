package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.MinecraftData
import at.hannibal2.skyhanni.utils.LorenzUtils
import kotlin.concurrent.fixedRateTimer

object HotSwapDetection {
    private val config get() = SkyHanniMod.feature.dev.debug

    private var latestTick = 0
    private var lastTps = 0
    private var hotswap = false

    init {
        fixedRateTimer(name = "skyhanni-tps-counter-seconds", period = 1000L) {
            val currentTick = MinecraftData.totalTicks
            val diff = currentTick - latestTick
            latestTick = currentTick

            // we count 2 client ticks per tick, we are bad
            handleTps(diff / 2)
        }
    }

    private fun handleTps(tps: Int) {
        if (!config.hotSwapDetection) return

        // ignore below one minute
        if (latestTick < 20 * 60) return

        println("diff: $tps")

        if (tps < 5) {
            LorenzUtils.debug("Lags! Only $tps tps")
        }

        if (!hotswap) {
            if (tps < 2) {
                if (lastTps > 15) {
                    LorenzUtils.debug("Detected hotswap now!")
                    hotswap = true
                }
            }
        } else {
            if (tps > 15) {
                hotswap = false
                LorenzUtils.debug("Detected hotswap end!")
            }
        }
        lastTps = tps
    }
}
