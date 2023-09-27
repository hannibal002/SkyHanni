package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.SkyBlockLeaveEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.text.SimpleDateFormat
import kotlin.time.Duration.Companion.seconds

class RealTimeFeatures {
    private val config get() = SkyHanniMod.feature.misc

    private val timeFormat24h = SimpleDateFormat("HH:mm:ss")
    private val timeFormat12h = SimpleDateFormat("hh:mm:ss a")

    private var latestSkyblockJoin = SimpleTimeMark.farPast()
    private var previousTime = 0.seconds

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!isEnabled()) return

        if (config.realTime) {
            val currentTime = (if(config.realTimeFormatToggle) timeFormat12h else timeFormat24h).format(System.currentTimeMillis())
            config.realTimePos.renderString(currentTime, posLabel = "Real Time")
        }

        if (config.timeOnSkyBlockSession) {
            val duration = TimeUtils.formatDuration(latestSkyblockJoin.passedSince() + previousTime)
            config.timeOnSkyBlockSessionPos.renderString("On SB since: $duration", posLabel = "Time on SkyBlock")
        }
    }

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        latestSkyblockJoin = SimpleTimeMark.now()
    }

    @SubscribeEvent
    fun onSkyBlockLeave(event: SkyBlockLeaveEvent) {
        LorenzUtils.debug("SkyBlockLeaveEvent")
        val lastTime = SimpleTimeMark.now().minus(latestSkyblockJoin)
        previousTime += lastTime
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.realTime
}