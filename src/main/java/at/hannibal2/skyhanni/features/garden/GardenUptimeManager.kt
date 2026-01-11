package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.garden.farming.CropClickEvent
import at.hannibal2.skyhanni.events.garden.pests.PestKillEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorOpenEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.features.garden.tracker.ArmorDropTracker
import at.hannibal2.skyhanni.features.garden.tracker.CropFeverTracker
import at.hannibal2.skyhanni.features.garden.tracker.PestProfitTracker
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.Stopwatch
import at.hannibal2.skyhanni.utils.tracker.GardenSession
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GardenUptimeManager {
    private val config get() = GardenApi.config.trackerUptimeSettings
    private val trackerSet: Set<SkyHanniTracker<*, *>> = setOf(ArmorDropTracker.tracker, PestProfitTracker, CropFeverTracker)
    private val afkTracker = Stopwatch()

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(config.types) {
            modify { it.update() }
        }
    }

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        modify { it.pauseSessionUptime() }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onTick(event: SkyHanniTickEvent) {
        if (!event.isMod(5)) return
        if (!afkTracker.isPaused()) {
            modify { it.update() }
        }
        if ((afkTracker.getLapTime() ?: return) >= config.afkTimeout.seconds) {
            modify { it.pauseSessionUptime() }
            afkTracker.pause()
        }
    }

    @HandleEvent
    fun onCropBreak(event: CropClickEvent) {
        // we do not want this tracker to be greedy, and exclude visitor/pest downtime whenever possible
        modify { it.swapActiveSession(SessionUptime.Garden(GardenSession.CROP), false) }
        afkTracker.start(true)
    }

    @HandleEvent
    fun onPestKill(event: PestKillEvent) {
        modify { it.swapActiveSession(SessionUptime.Garden(GardenSession.PEST)) }
        afkTracker.start(true)
    }

    @HandleEvent
    fun onVisitorOpen(event: VisitorOpenEvent) {
        modify { it.swapActiveSession(SessionUptime.Garden(GardenSession.VISITOR)) }
        afkTracker.start(true)
    }

    fun modify(modifyFunction: (SkyHanniTracker<*, *>) -> Unit) {
        trackerSet.forEach(modifyFunction)
    }
}
