package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.enums.OutsideSbFeature
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.WinterAPI
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.RecalculatingValue
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.asTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.text.SimpleDateFormat
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

class TimeFeatures {

    private val config get() = SkyHanniMod.feature.gui
    private val winterConfig get() = SkyHanniMod.feature.event.winter

    private val timeFormat24h = SimpleDateFormat("HH:mm:ss")
    private val timeFormat12h = SimpleDateFormat("hh:mm:ss a")

    private val startOfNextYear = RecalculatingValue(1.seconds) {
        SkyBlockTime(year = SkyBlockTime.now().year + 1).asTimeMark()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!LorenzUtils.inSkyBlock && !OutsideSbFeature.REAL_TIME.isSelected()) return

        if (config.realTime) {
            val currentTime =
                (if (config.realTimeFormatToggle) timeFormat12h else timeFormat24h).format(System.currentTimeMillis())
            config.realTimePosition.renderString(currentTime, posLabel = "Real Time")
        }

        if (winterConfig.islandCloseTime && IslandType.WINTER.isInIsland()) {
            if (WinterAPI.isDecember()) return
            val timeTillNextYear = startOfNextYear.getValue().timeUntil()
            val alreadyInNextYear = timeTillNextYear > 5.days
            val text = if (alreadyInNextYear) {
                "§fJerry's Workshop §cis closing!"
            } else {
                "§fJerry's Workshop §ecloses in §b${timeTillNextYear.format()}"
            }
            winterConfig.islandCloseTimePosition.renderString(text, posLabel = "Winter Time")
        }
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "misc.timeConfigs.winterTime", "event.winter.islandCloseTime")
        event.move(2, "misc.timeConfigs.winterTimePos", "event.winter.islandCloseTimePosition")

        event.move(2, "misc.timeConfigs.realTime", "gui.realTime")
        event.move(2, "misc.timeConfigs.realTimePos", "gui.realTimePosition")
    }
}
