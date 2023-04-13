package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.TimeUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class GardenVisitorTimer {
    private val patternNextVisitor = Pattern.compile(" Next Visitor: §r§b(.*)")
    private val patternVisitors = Pattern.compile("§b§lVisitors: §r§f\\((\\d)\\)")
    private var render = ""
    private var last = 0.seconds
    private var lastVisitors = 0

    @SubscribeEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        if (!isEnabled()) return

        var visitorsAmount = 0
        var current = 15.minutes
        var queueFull = false
        for (line in event.tabList) {
            var matcher = patternNextVisitor.matcher(line)
            if (matcher.matches()) {
                val rawTime = matcher.group(1)
                current = TimeUtils.getDuration(rawTime)
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

        val diff = (last - current).inWholeSeconds
        if (diff == 0L && visitorsAmount == lastVisitors) return
        last = current
        lastVisitors = visitorsAmount

        val extraSpeed = if (diff in 2..10) {
            val d = current / diff.toDouble()
            "§7/§e" + TimeUtils.formatDuration(d)
        } else ""

        val formatDuration = TimeUtils.formatDuration(current)
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