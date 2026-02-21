package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorCountChangeEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorNextArrivalChangeEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GardenVisitorTimer {

    private val config get() = VisitorApi.config.timer

    private var display: Renderable? = null
    private var nextArrivalMark: SimpleTimeMark? = null
    private var lastSixthVisitorWarning: SimpleTimeMark = SimpleTimeMark.farPast()

    @HandleEvent(ProfileJoinEvent::class)
    fun onProfileJoin() {
        display = null
    }

    @HandleEvent
    fun onVisitorCountChange(event: VisitorCountChangeEvent) {
        if (event.visitorCount != 6 || !config.sixthVisitorWarning) return
        // Do not warn immediately on world switch
        if (SkyBlockUtils.lastWorldSwitch.passedSince() < 3.seconds) return

        if (lastSixthVisitorWarning.passedSince() < 2.minutes) return
        lastSixthVisitorWarning = SimpleTimeMark.now()
        TitleManager.sendTitle("§a6th Visitor Ready")
        SoundUtils.playBeepSound()
    }

    @HandleEvent
    fun onVisitorNextArrivalChange(event: VisitorNextArrivalChangeEvent) {
        nextArrivalMark = event.nextVisitor.takeIf { it.isInFuture() && it != nextArrivalMark } ?: return
        display = buildDisplay()
    }

    private fun buildDisplay(): Renderable? {
        if (!VisitorApi.visitorsUnlocked) return Renderable.text("§cVisitors not unlocked!")

        val nextArrivalMark = nextArrivalMark ?: return null
        val formatColor = if (VisitorApi.queueFull) "6" else "e"

        val adjustedArrivalMark = if (GardenApi.isCurrentlyFarming()) {
            val adjustedTimeUntil = nextArrivalMark.timeUntil() / 3
            SimpleTimeMark.now() + adjustedTimeUntil
        } else nextArrivalMark

        val extraSpeed = if (GardenApi.isCurrentlyFarming()) {
            val duration = adjustedArrivalMark.timeUntil() * (GardenCropSpeed.getRecentBPS() / 20)
            "§7/§$formatColor" + duration.format()
        } else ""

        val formatDuration = adjustedArrivalMark.timeUntil().format()
        val next = if (VisitorApi.queueFull && (!config.sixthVisitorEnabled || adjustedArrivalMark.isInPast())) "§cQueue Full!" else {
            "Next in §$formatColor$formatDuration$extraSpeed"
        }
        val visitorLabel = StringUtils.pluralize(VisitorApi.visitorCount, "visitor")
        return Renderable.clickable(
            "§b${VisitorApi.visitorCount} $visitorLabel §7($next§7)",
            tips = listOf("§eClick to teleport to the barn!"),
            onLeftClick = { HypixelCommands.teleportToPlot("barn") },
        )
    }

    init {
        RenderDisplayHelper(
            condition = config::enabled,
            outsideInventory = true,
            inOwnInventory = true,
            onlyOnIsland = IslandType.GARDEN,
        ) {
            config.position.renderRenderable(display, posLabel = "Garden Visitor Timer")
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.visitorTimerEnabled", "garden.visitors.timer.enabled")
        event.move(3, "garden.visitorTimerSixthVisitorEnabled", "garden.visitors.timer.sixthVisitorEnabled")
        event.move(3, "garden.visitorTimerSixthVisitorWarning", "garden.visitors.timer.sixthVisitorWarning")
        event.move(3, "garden.visitorTimerPos", "garden.visitors.timer.pos")

        event.move(87, "garden.visitors.timer.pos", "garden.visitors.timer.position")
    }
}
