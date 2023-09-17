package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.concurrent.fixedRateTimer

class TpsCounter {
    companion object {
        private const val minDataAmount = 5
        private const val waitAfterWorldSwitch = 6
    }

    private var packetsFromLastSecond = 0
    private var tpsList = mutableListOf<Int>()
    private var ignoreFirstTicks = waitAfterWorldSwitch
    private var hasPacketReceived = false

    private var display = ""

    init {
        fixedRateTimer(name = "skyhanni-tps-counter-seconds", period = 1000L) {
            if (!LorenzUtils.inSkyBlock) return@fixedRateTimer
            if (!SkyHanniMod.feature.misc.tpsDisplayEnabled) return@fixedRateTimer
            if (packetsFromLastSecond == 0) return@fixedRateTimer

            if (ignoreFirstTicks > 0) {
                ignoreFirstTicks--
                val current = ignoreFirstTicks + minDataAmount
                display = "§eTPS: §f(${current}s)"
                packetsFromLastSecond = 0
                return@fixedRateTimer
            }

            tpsList.add(packetsFromLastSecond)
            packetsFromLastSecond = 0
            if (tpsList.size > 10) {
                tpsList = tpsList.drop(1).toMutableList()
            }

            display = if (tpsList.size < minDataAmount) {
                val current = minDataAmount - tpsList.size
                "§eTPS: §f(${current}s)"
            } else {
                val sum = tpsList.sum().toDouble()
                var tps = (sum / tpsList.size).round(1)
                if (tps > 20) tps = 20.0
                val color = getColor(tps)
                "§eTPS: $color$tps"
            }
        }
        fixedRateTimer(name = "skyhanni-tps-counter-ticks", period = 50L) {
            if (!LorenzUtils.inSkyBlock) return@fixedRateTimer
            if (!SkyHanniMod.feature.misc.tpsDisplayEnabled) return@fixedRateTimer

            if (hasPacketReceived) {
                hasPacketReceived = false
                packetsFromLastSecond++
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        tpsList.clear()
        packetsFromLastSecond = 0
        ignoreFirstTicks = waitAfterWorldSwitch
        display = ""
    }

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    fun onChatPacket(event: PacketEvent.ReceiveEvent) {
        if (!SkyHanniMod.feature.misc.tpsDisplayEnabled) return
        hasPacketReceived = true
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.misc.tpsDisplayEnabled) return

        SkyHanniMod.feature.misc.tpsDisplayPosition.renderString(display, posLabel = "Tps Display")
    }

    private fun getColor(tps: Double): String {
        return if (tps > 19.8) {
            "§2"
        } else if (tps > 19) {
            "§a"
        } else if (tps > 17.5) {
            "§6"
        } else if (tps > 12) {
            "§c"
        } else {
            "§4"
        }
    }
}
