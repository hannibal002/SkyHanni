package at.hannibal2.hanni.features.nether

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.model.TabWidget
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.events.WidgetUpdateEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.RegexUtils.matchMatcher
import at.hannibal2.hanni.utils.RenderUtils.renderString
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@HanniModule
object VolcanoExplosivityDisplay {

    private val config get() = HanniMod.feature.crimsonIsle
    private val patternGroup = RepoPattern.group("crimson.volcano")

    /**
     * REGEX-TEST:  Volcano: §r§8INACTIVE
     */
    private val statusPattern by patternGroup.pattern(
        "tablistline",
        " *Volcano: (?<status>(?:§.)*\\S+)",
    )
    private var display = ""

    @HandleEvent
    fun onWidgetUpdate(event: WidgetUpdateEvent) {
        if (!isEnabled()) return
        if (!event.isWidget(TabWidget.VOLCANO)) return

        if (event.isClear()) {
            display = ""
            return
        }
        // TODO merge widget pattern with statusPattern
        statusPattern.matchMatcher(event.lines.first()) {
            display = "§bVolcano Explosivity§7: ${group("status")}"
        }
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        config.positionVolcano.renderString(display, posLabel = "Volcano Explosivity")
    }

    private fun isEnabled() = IslandType.CRIMSON_ISLE.isCurrent() && config.volcanoExplosivity
}
