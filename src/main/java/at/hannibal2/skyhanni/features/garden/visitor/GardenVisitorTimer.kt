package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.CropClickEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.PreProfileSwitchEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorArrivalEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.asTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TabListData
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.concurrent.fixedRateTimer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class GardenVisitorTimer {
    private val config get() = SkyHanniMod.feature.garden.visitors.timer
    private val pattern = "§b§lVisitors: §r§f\\((?<time>.*)\\)".toPattern()
    private var render = ""
    private var lastMillis = 0.seconds
    private var sixthVisitorArrivalTime = SimpleTimeMark.farPast()
    private var visitorJustArrived = false
    private var sixthVisitorReady = false
    private var lastTimerValue = ""
    private var lastTimerUpdate = SimpleTimeMark.farPast()

    //TODO nea?
//    private val visitorInterval by dynamic(GardenAPI::config, Storage.ProfileSpecific.GardenStorage::visitorInterval)
    private var visitorInterval: Duration?
        get() = GardenAPI.storage?.visitorInterval?.toDuration(DurationUnit.MILLISECONDS)
        set(value) {
            value?.let {
                GardenAPI.storage?.visitorInterval = it.inWholeMilliseconds
            }
        }

    companion object {
        var lastVisitors: Int = -1
    }

    @SubscribeEvent
    fun onVisitorArrival(event: VisitorArrivalEvent) {
        visitorJustArrived = true
    }

    init {
        fixedRateTimer(name = "skyhanni-update-visitor-display", period = 1000L) {
            try {
                updateVisitorDisplay()
            } catch (error: Throwable) {
                ErrorManager.logError(error, "Encountered an error when updating visitor display")
            }
            try {
                GardenVisitorDropStatistics.saveAndUpdate()
            } catch (_: Throwable) {
            } // no config yet
        }
    }

    @SubscribeEvent
    fun onPreProfileSwitch(event: PreProfileSwitchEvent) {
        render = ""
        lastMillis = 0.seconds
        sixthVisitorArrivalTime = SimpleTimeMark.farPast()
        visitorJustArrived = false
        sixthVisitorReady = false
    }

    private fun updateVisitorDisplay() {
        if (!isEnabled()) return

        var visitorsAmount = VisitorAPI.visitorsInTabList(TabListData.getTabList()).size
        var visitorInterval = visitorInterval ?: return
        var millis = visitorInterval
        var queueFull = false
        for (line in TabListData.getTabList()) {
            if (line == "§b§lVisitors: §r§f(§r§c§lQueue Full!§r§f)") {
                queueFull = true
                continue
            }
            if (line == "§b§lVisitors: §r§cNot Unlocked!") {
                render = ""
                return
            }

            pattern.matchMatcher(line) {
                val rawTime = group("time").removeColor()
                if (lastTimerValue != rawTime) {
                    lastTimerUpdate = SimpleTimeMark.now()
                    lastTimerValue = rawTime
                }
                millis = TimeUtils.getDuration(rawTime)
            }
        }

        if (lastVisitors != -1 && visitorsAmount - lastVisitors == 1) {
            if (!queueFull) {
                visitorInterval = millis
                this.visitorInterval = visitorInterval
            } else {
                updateSixthVisitorArrivalTime()
            }
        }

        if (queueFull) {
            if (visitorJustArrived && visitorsAmount - lastVisitors == 1) {
                updateSixthVisitorArrivalTime()
                visitorJustArrived = false
                sixthVisitorReady = false
            }
            millis = sixthVisitorArrivalTime.timeUntil()

            val nextSixthVisitorArrival = SimpleTimeMark.now() + millis + (visitorInterval * (5 - visitorsAmount))
            GardenAPI.storage?.nextSixthVisitorArrival = nextSixthVisitorArrival.toMillis()
            if (isSixthVisitorEnabled() && millis.isNegative()) {
                visitorsAmount++
                if (!sixthVisitorReady) {
                    LorenzUtils.sendTitle("§a6th Visitor Ready", 5.seconds)
                    sixthVisitorReady = true
                    if (isSixthVisitorWarningEnabled()) SoundUtils.playBeepSound()
                }
            }
        }
        val sinceLastTimerUpdate = lastTimerUpdate.passedSince() - 100.milliseconds
        val guessTime = visitorsAmount < 5 && sinceLastTimerUpdate in 500.milliseconds..60.seconds
        if (guessTime) {
            millis -= sinceLastTimerUpdate
        }

        if (lastMillis == INFINITE) {
            LorenzUtils.error("Found Visitor Timer bug, reset value (lastMillis was infinite).")
            lastMillis = 0.seconds
        }

        val diff = lastMillis - millis
        if (diff == 0.seconds && visitorsAmount == lastVisitors) return
        lastMillis = millis
        lastVisitors = visitorsAmount

        val formatColor = when {
            queueFull -> "6"
            else -> "e"
        }

        val extraSpeed = if (diff in 2.seconds..10.seconds) {
            val factor = diff.inWholeSeconds.toDouble()
            val duration = millis / factor
            "§7/§$formatColor" + duration.format()
        } else ""
        if (config.newVisitorPing && millis < 10.seconds) {
            SoundUtils.playBeepSound()
        }

        val formatDuration = TimeUtils.formatDuration(millis)
        val next = if (queueFull && (!isSixthVisitorEnabled() || millis.isNegative())) "§cQueue Full!" else {
            "Next in §$formatColor$formatDuration$extraSpeed"
        }
        val visitorLabel = if (visitorsAmount == 1) "visitor" else "visitors"
        render = "§b$visitorsAmount $visitorLabel §7($next§7)"
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        config.pos.renderString(render, posLabel = "Garden Visitor Timer")
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        lastVisitors = -1
        GardenAPI.storage?.nextSixthVisitorArrival?.let {
            sixthVisitorArrivalTime = it.asTimeMark()
        }
        sixthVisitorReady = false
        lastMillis = sixthVisitorArrivalTime.timeUntil()
    }

    @SubscribeEvent
    fun onBlockBreak(event: CropClickEvent) {
        if (!isEnabled()) return
        sixthVisitorArrivalTime -= 100.milliseconds
        lastTimerUpdate -= 100.milliseconds
    }

    private fun updateSixthVisitorArrivalTime() {
        visitorInterval?.let {
            sixthVisitorArrivalTime = SimpleTimeMark.now() + it
        }
    }

    private fun isSixthVisitorEnabled() = config.sixthVisitorEnabled
    private fun isSixthVisitorWarningEnabled() = config.sixthVisitorWarning
    private fun isEnabled() = GardenAPI.inGarden() && config.enabled

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.visitorTimerEnabled", "garden.visitors.timer.enabled")
        event.move(3, "garden.visitorTimerSixthVisitorEnabled", "garden.visitors.timer.sixthVisitorEnabled")
        event.move(3, "garden.visitorTimerSixthVisitorWarning", "garden.visitors.timer.sixthVisitorWarning")
        event.move(3, "garden.visitorTimerPos", "garden.visitors.timer.pos")
    }
}
