package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.garden.farming.CropClickEvent
import at.hannibal2.skyhanni.events.garden.pests.PestKillEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorArrivalEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TabListData
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.StringRenderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@SkyHanniModule
object GardenVisitorTimer {

    private val config get() = VisitorApi.config.timer

    /**
     * REGEX-TEST:  Next Visitor: §r§b11m
     * REGEX-TEST:  Next Visitor: §r§c§lQueue Full!
     */
    private val timePattern by RepoPattern.pattern(
        "garden.visitor.timer.time.new",
        " Next Visitor: §r(?<info>.*)",
    )

    private var display: Renderable? = null
    private var lastMillis = 0.seconds
    private var sixthVisitorArrivalTime = SimpleTimeMark.farPast()
    private var visitorJustArrived = false
    private var lastSixthVisitorWarning = SimpleTimeMark.farPast()
    private var lastTimerValue = ""
    private var lastTimerUpdate = SimpleTimeMark.farPast()
    private var lastVisitors: Int = -1

    // TODO nea?
    // private val visitorInterval by dynamic(GardenAPI::config, Storage.ProfileSpecific.GardenStorage::visitorInterval)
    private var visitorInterval: Duration?
        get() = GardenApi.storage?.visitorInterval?.toDuration(DurationUnit.MILLISECONDS)
        set(value) {
            value?.let {
                GardenApi.storage?.visitorInterval = it.inWholeMilliseconds
            }
        }

    @HandleEvent
    fun onVisitorArrival(event: VisitorArrivalEvent) {
        visitorJustArrived = true
    }

    @HandleEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        display = null
        lastMillis = 0.seconds
        sixthVisitorArrivalTime = SimpleTimeMark.farPast()
        visitorJustArrived = false
    }

    // TODO split up into multiple smaller functions
    @Suppress("CyclomaticComplexMethod")
    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onSecondPassed(event: SecondPassedEvent) {
        var visitorsAmount = VisitorApi.visitorsInTabList(TabListData.getTabList()).size
        var visitorInterval = visitorInterval ?: return
        var millis = visitorInterval
        var queueFull = false

        timePattern.firstMatcher(TabListData.getTabList()) {
            val timeInfo = group("info").removeColor()
            if (timeInfo == "Not Unlocked!") {
                display = StringRenderable("§cVisitors not unlocked!")
                return
            }
            if (timeInfo == "Queue Full!") {
                queueFull = true
            } else {
                if (lastTimerValue != timeInfo) {
                    lastTimerUpdate = SimpleTimeMark.now()
                    lastTimerValue = timeInfo
                }
                millis = TimeUtils.getDuration(timeInfo)
            }
        } ?: run {
            display = createDisplayText("§cVisitor time info not in tab list")
            return
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
            }
            millis = sixthVisitorArrivalTime.timeUntil()

            val nextSixthVisitorArrival = SimpleTimeMark.now() + millis + (visitorInterval * (5 - visitorsAmount))
            GardenApi.storage?.nextSixthVisitorArrival = nextSixthVisitorArrival
            if (millis.isNegative()) {
                visitorsAmount++
                if (config.sixthVisitorWarning) {
                    warn6thVisitor()
                }
            }
        }
        val sinceLastTimerUpdate = lastTimerUpdate.passedSince() - 100.milliseconds
        val guessTime = visitorsAmount < 5 && sinceLastTimerUpdate in 500.milliseconds..60.seconds
        if (guessTime) {
            millis -= sinceLastTimerUpdate
        }

        if (lastMillis == Duration.INFINITE) {
            ErrorManager.logErrorStateWithData(
                "Found Visitor Timer bug, reset value", "lastMillis was infinite",
                "lastMillis" to lastMillis,
            )
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

        val adjustedMillis = if (GardenApi.isCurrentlyFarming()) millis / 3 else millis
        val extraSpeed = if (GardenApi.isCurrentlyFarming()) {
            val duration = adjustedMillis * (GardenCropSpeed.getRecentBPS() / 20)
            "§7/§$formatColor" + duration.format()
        } else ""
        if (config.newVisitorPing && adjustedMillis < 10.seconds) {
            SoundUtils.playBeepSound()
        }

        val formatDuration = millis.format()
        val next = if (queueFull && (!config.sixthVisitorEnabled || millis.isNegative())) "§cQueue Full!" else {
            "Next in §$formatColor$formatDuration$extraSpeed"
        }
        val visitorLabel = if (visitorsAmount == 1) "visitor" else "visitors"
        display = createDisplayText("§b$visitorsAmount $visitorLabel §7($next§7)")
    }

    private fun warn6thVisitor() {
        // do not warn immediately on world switch
        if (SkyBlockUtils.lastWorldSwitch.passedSince() < 3.seconds) return

        if (lastSixthVisitorWarning.passedSince() < 2.minutes) return
        lastSixthVisitorWarning = SimpleTimeMark.now()
        TitleManager.sendTitle("§a6th Visitor Ready")
        SoundUtils.playBeepSound()
    }

    private fun createDisplayText(text: String) = Renderable.clickable(
        text,
        tips = listOf("§eClick to teleport to the barn!"),
        onLeftClick = { HypixelCommands.teleportToPlot("barn") },
    )

    init {
        RenderDisplayHelper(
            condition = { isEnabled() },
            outsideInventory = true,
            inOwnInventory = true,
        ) {
            config.position.renderRenderable(display, posLabel = "Garden Visitor Timer")
        }
    }

    @HandleEvent
    fun onWorldChange() {
        lastVisitors = -1
        GardenApi.storage?.nextSixthVisitorArrival?.let {
            if (it.isFarFuture() && it.toMillis() != -9223370336633802065) {
                sixthVisitorArrivalTime = it
            }
        }
        lastMillis = sixthVisitorArrivalTime.timeUntil()
    }

    @HandleEvent
    fun onCropClick(event: CropClickEvent) {
        if (!isEnabled()) return
        sixthVisitorArrivalTime -= 100.milliseconds

        // We only need manually retracting the time when hypixel shows 6 minutes or above
        if (lastMillis > 5.minutes) {
            lastTimerUpdate -= 100.milliseconds
        }
    }

    @HandleEvent
    fun onPestKill(event: PestKillEvent) {
        if (!isEnabled()) return
        sixthVisitorArrivalTime -= 30.seconds

        if (lastMillis > 5.minutes) {
            lastTimerUpdate -= 30.seconds
        }
    }

    private fun updateSixthVisitorArrivalTime() {
        visitorInterval?.let {
            sixthVisitorArrivalTime = SimpleTimeMark.now() + it
        }
    }

    private fun isEnabled() = GardenApi.inGarden() && config.enabled

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.visitorTimerEnabled", "garden.visitors.timer.enabled")
        event.move(3, "garden.visitorTimerSixthVisitorEnabled", "garden.visitors.timer.sixthVisitorEnabled")
        event.move(3, "garden.visitorTimerSixthVisitorWarning", "garden.visitors.timer.sixthVisitorWarning")
        event.move(3, "garden.visitorTimerPos", "garden.visitors.timer.pos")

        event.move(87, "garden.visitors.timer.pos", "garden.visitors.timer.position")
    }
}
