package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.features.garden.CropType.Companion.getCropType
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.TimeUtils
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern
import kotlin.math.roundToLong

class GardenVisitorTimer {
    private val patternNextVisitor = Pattern.compile(" Next Visitor: §r§b(.*)")
    private val patternVisitors = Pattern.compile("§b§lVisitors: §r§f\\((\\d)\\)")
    private var render = ""
    private var lastMillis = 0L
    private var lastVisitors: Int = -1
    private var sixthVisitorArrivalTime: Long = 0
    private var visitorInterval
        get() = SkyHanniMod.feature.hidden.visitorInterval
        set(value) { SkyHanniMod.feature.hidden.visitorInterval = value }

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onTabListUpdate(event: TabListUpdateEvent) {
        if (!isEnabled()) return

        var visitorsAmount = 0
        var millis = visitorInterval
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

        if (lastVisitors != -1 && visitorsAmount - lastVisitors == 1) {
            if (!queueFull) {
                visitorInterval = ((millis - 1) / 60_000L + 1) * 60_000L
            } else {
                sixthVisitorArrivalTime = System.currentTimeMillis() + visitorInterval
            }
        }

        if (queueFull) {
            millis = sixthVisitorArrivalTime - System.currentTimeMillis()
        }

        val diff = lastMillis - millis
        if (diff == 0L && visitorsAmount == lastVisitors) return
        lastMillis = millis
        lastVisitors = visitorsAmount

        val formatColor = if (queueFull) "6" else "e"

        val extraSpeed = if (diff in 2000..10_000) {
            val factor = diff / 1000.0
            "§7/§$formatColor" + TimeUtils.formatDuration((millis / factor).roundToLong())
        } else ""

        val formatDuration = TimeUtils.formatDuration(millis)
        val next = if (queueFull && (!isSixthVisitorEnabled() || millis < 0)) "§cQueue Full!" else {
                "Next in §$formatColor$formatDuration$extraSpeed"
            }
        val visitorLabel = if (visitorsAmount == 1) "visitor" else "visitors"
        render = "§b$visitorsAmount $visitorLabel §7($next§7)"
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return

        SkyHanniMod.feature.garden.visitorTimerPos.renderString(render, posLabel = "Garden Visitor Timer")
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        lastVisitors = -1
        sixthVisitorArrivalTime = 0
    }

    @SubscribeEvent
    fun onBlockBreak(event: BlockClickEvent) {
        if (!isEnabled() || event.getBlockState.getCropType() == null) return
        sixthVisitorArrivalTime -= 100
    }

    private fun isSixthVisitorEnabled() = SkyHanniMod.feature.garden.visitorTimerSixthVisitorEnabled
    private fun isEnabled() = GardenAPI.inGarden() && SkyHanniMod.feature.garden.visitorTimerEnabled
}