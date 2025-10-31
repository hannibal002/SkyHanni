package at.hannibal2.hanni.features.misc

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.config.enums.OutsideSBFeature
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.WinterApi
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.RecalculatingValue
import at.hannibal2.hanni.utils.RenderUtils.renderString
import at.hannibal2.hanni.utils.SkyBlockTime
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.TimeUtils.format
import java.text.SimpleDateFormat
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

@HanniModule
object TimeFeatures {

    private val config get() = HanniMod.feature.gui
    private val winterConfig get() = HanniMod.feature.event.winter

    private val startOfNextYear by RecalculatingValue(1.seconds) {
        SkyBlockTime(year = SkyBlockTime.now().year + 1).toTimeMark()
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        @Suppress("InSkyBlockEarlyReturn")
        if (!SkyBlockUtils.inSkyBlock && !OutsideSBFeature.REAL_TIME.isSelected()) return
        if (config.realTime) {
            val timeFormat = if (config.realTimeFormatToggle) {
                // 12 h format
                SimpleDateFormat("hh:mm${if (config.realTimeShowSeconds) ":ss" else ""} a")
            } else {
                // 24 h format
                SimpleDateFormat("HH:mm${if (config.realTimeShowSeconds) ":ss" else ""}")
            }
            val currentTime = timeFormat.format(System.currentTimeMillis())
            config.realTimePosition.renderString(currentTime, posLabel = "Real Time")
        }

        if (winterConfig.islandCloseTime && IslandType.WINTER.isCurrent()) {
            if (WinterApi.isDecember()) return
            val timeTillNextYear = startOfNextYear.timeUntil()
            val alreadyInNextYear = timeTillNextYear > 5.days
            val text = if (alreadyInNextYear) {
                "§fJerry's Workshop §cis closing!"
            } else {
                "§fJerry's Workshop §ecloses in §b${timeTillNextYear.format()}"
            }
            winterConfig.islandCloseTimePosition.renderString(text, posLabel = "Winter Time")
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "misc.timeConfigs.winterTime", "event.winter.islandCloseTime")
        event.move(2, "misc.timeConfigs.winterTimePos", "event.winter.islandCloseTimePosition")

        event.move(2, "misc.timeConfigs.realTime", "gui.realTime")
        event.move(2, "misc.timeConfigs.realTimePos", "gui.realTimePosition")
    }
}
