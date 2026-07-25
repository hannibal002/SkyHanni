package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandTypeTag
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object ForagingBeaconWarning {

    private var lastAlert = SimpleTimeMark.farPast()

    val beaconReadyPattern by RepoPattern.pattern(
        "foraging.beacon.available",
        " Cooldown: AVAILABLE",
    )

    @HandleEvent(onlyOnIslandTypeTag = [IslandTypeTag.FORAGING_CUSTOM_TREES])
    fun onWidget(event: WidgetUpdateEvent) {
        if (!isEnabled()) return
        if (!event.isWidget(TabWidget.MOONGLADE_BEACON)) return
        if (lastAlert.passedSince() < 9.minutes) return
        beaconReadyPattern.firstMatcher(event.lines.map { it.string }) {
            TitleManager.sendTitle("§aBeacon Ready")
            SoundUtils.playPlingSound()
            lastAlert = SimpleTimeMark.now()
        }
    }

    fun isEnabled() = SkyHanniMod.feature.foraging.foragingBeacon.beaconAlert

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(139, "foraging.moongladeBeacon", "foraging.foragingBeacon")
    }

}
