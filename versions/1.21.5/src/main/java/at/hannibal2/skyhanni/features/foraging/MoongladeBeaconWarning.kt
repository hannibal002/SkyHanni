package at.hannibal2.hanni.features.foraging

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.model.TabWidget
import at.hannibal2.hanni.data.title.TitleManager
import at.hannibal2.hanni.events.WidgetUpdateEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ModernPatterns
import at.hannibal2.hanni.utils.RegexUtils.firstMatcher
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.SoundUtils
import kotlin.time.Duration.Companion.minutes

@HanniModule
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

    fun isEnabled() = HanniMod.feature.foraging.moongladeBeacon.beaconAlert

}
