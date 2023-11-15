package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.MinecraftData
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import kotlin.concurrent.fixedRateTimer

object HotSwapDetection {
    private val config get() = SkyHanniMod.feature.dev.debug

    private var latestTick = 0
    private var beforeThatTick = 0
    private var lastTps = 0
    private var hotswap = false

    init {
        // TODO seems broken somehow?
        fixedRateTimer(name = "skyhanni-hot-swap-detection", period = 250) {
            val currentTick = MinecraftData.totalTicks
            val diff = currentTick - latestTick
            latestTick = currentTick

            // we count 2 client ticks per tick, we are bad
            handleTps(diff * 2)
        }
    }

    private fun handleTps(tps: Int) {
        Minecraft.getMinecraft().theWorld ?: return
        if (!config.hotSwapDetection) return

        // ignore below one minute
        if (latestTick < 20 * 60) return

        if (!hotswap) {
            if (tps < 5) {
                if (beforeThatTick > 18) {
                    LorenzUtils.debug("Detected hotswap now!")
                    hotswap = true
                }
            }
        } else {
            if (tps > 15) {
                hotswap = false
            }
        }
        beforeThatTick = lastTps
        lastTps = tps
    }
}
