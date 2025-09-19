package at.hannibal2.skyhanni.features.garden.tracker

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.garden.farming.CropClickEvent
import at.hannibal2.skyhanni.events.garden.pests.PestKillEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorOpenEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.Stopwatch
import at.hannibal2.skyhanni.utils.tracker.GardenSession
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GardenUptimeTracker {
    val trackerSet = setOf(ArmorDropTracker.tracker, DicerRngDropTracker.tracker, PestProfitTracker.tracker)

    val afkTracker = Stopwatch()

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        if (!event.isMod(5)) return
        if (!afkTracker.isPaused()) {
            trackerSet.forEach { it.update() }
        }
        if ((afkTracker.getLapTime() ?: return) >= 15.seconds) {
            trackerSet.forEach { it.pauseSessionUptime() }
            afkTracker.pause()
        }
    }

    @HandleEvent
    fun onCropBreak(event: CropClickEvent) {
        // we do not want this tracker to be greedy, and exclude visitor/pest downtime whenever possible
        trackerSet.forEach { it.swapActiveSession(SessionUptime.Garden(GardenSession.CROP), false) }
        afkTracker.start(true)
    }

    @HandleEvent
    fun onPestKill(event: PestKillEvent) {
        trackerSet.forEach { it.swapActiveSession(SessionUptime.Garden(GardenSession.PEST)) }
        afkTracker.start(true)
    }

    @HandleEvent
    fun onVisitorOpen(event: VisitorOpenEvent) {
        trackerSet.forEach { it.swapActiveSession(SessionUptime.Garden(GardenSession.VISITOR)) }
        afkTracker.start(true)
    }
}
