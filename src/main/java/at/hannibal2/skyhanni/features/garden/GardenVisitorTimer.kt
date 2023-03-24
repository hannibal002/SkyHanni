package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.TimeUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class GardenVisitorTimer {
    private val patternNextVisitor = Pattern.compile(" Next Visitor: §r§b(.*)")
    private val patternVisitors = Pattern.compile("§b§lVisitors: §r§f\\((\\d)\\)")
    private var render = ""
    private var lastMillis = 0L
    private var lastVisitors = 0

    @SubscribeEvent
    fun onTick(event: TabListUpdateEvent) {
        if (!isEnabled()) return

        var visitorsAmount = 0
        var millis = 15 * 60_000L
        var queueFull = false
        for (line in event.tabList) {
            var matcher = patternNextVisitor.matcher(line)
            if (matcher.matches()) {
                val rawTime = matcher.group(1)
                millis = TimeUtils.getMillis(rawTime)
            } else if (line == " Next Visitor: §r§c§lQueue Full!") {
                queueFull = true
            } else if (line == " Next Visitor: §r§cNot Unlocked!") {
                render = ""
                return
            }

            matcher = patternVisitors.matcher(line)
            if (matcher.matches()) {
                visitorsAmount = matcher.group(1).toInt()
            }
        }

        val diff = lastMillis - millis
        if (diff == 0L && visitorsAmount == lastVisitors) return
        lastMillis = millis
        lastVisitors = visitorsAmount

        val extraSpeed = if (diff in 1001..10_000) {
            val factor = diff / 1000
            "§7/§e" + TimeUtils.formatDuration(millis / factor)
        } else ""

        val formatDuration = TimeUtils.formatDuration(millis)
        val next = if (queueFull) "§cQueue Full!" else {
            "Next in §e$formatDuration$extraSpeed"
        }
        val visitorLabel = if (visitorsAmount == 1) "visitor" else "visitors"
        render = "§b$visitorsAmount $visitorLabel §7($next§7)"
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return

        SkyHanniMod.feature.garden.visitorTimerPos.renderString(render, posLabel = "Garden Visitor Timer")
    }

    private fun isEnabled() = GardenAPI.inGarden() && SkyHanniMod.feature.garden.visitorTimerEnabled
}