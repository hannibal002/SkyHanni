package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ModernPatterns
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object MoongladeBeaconWarning {

    private var lastAlert = SimpleTimeMark.farPast()

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onWidget(event: WidgetUpdateEvent) {
        if (!isEnabled()) return
        if (!event.isWidget(TabWidget.MOONGLADE_BEACON)) return
        if (lastAlert.passedSince() < 9.minutes) return
        ModernPatterns.beaconReadyPattern.firstMatcher(event.lines) {
            TitleManager.sendTitle("§aBeacon Ready")
            SoundUtils.playPlingSound()
            lastAlert = SimpleTimeMark.now()
        }
    }

    fun isEnabled() = SkyHanniMod.feature.foraging.moongladeBeacon.beaconAlert

}
