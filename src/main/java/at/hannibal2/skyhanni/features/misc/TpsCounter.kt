package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
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
    private val config get() = SkyHanniMod.feature.gui

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
            if (!config.tpsDisplay) return@fixedRateTimer
            if (packetsFromLastSecond == 0) return@fixedRateTimer

            if (ignoreFirstTicks > 0) {
                ignoreFirstTicks--
                val current = ignoreFirstTicks + minDataAmount
                display = "§eTps: §f(${current}s)"
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
                "§eTps: §f(${current}s)"
            } else {
                val sum = tpsList.sum().toDouble()
                var tps = (sum / tpsList.size).round(1)
                if (tps > 20) tps = 20.0
                val color = getColor(tps)
                "§eTps: $color$tps"
            }
        }
        fixedRateTimer(name = "skyhanni-tps-counter-ticks", period = 50L) {
            if (!LorenzUtils.inSkyBlock) return@fixedRateTimer
            if (!config.tpsDisplay) return@fixedRateTimer

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
        if (!config.tpsDisplay) return
        hasPacketReceived = true
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.tpsDisplay) return

        config.tpsDisplayPosition.renderString(display, posLabel = "Tps Display")
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "misc.tpsDisplayEnabled", "gui.tpsDisplay")
        event.move(2, "misc.tpsDisplayPosition", "gui.tpsDisplayPosition")
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
