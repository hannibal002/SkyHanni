package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.matchAllComponents
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object OverbloomDisplay {

    private val config get() = GardenApi.config

    private val patternGroup = RepoPattern.group("stats.tablist.no-color")

    /**
     * REGEX-TEST:  Overbloom: ☀84
     * REGEX-TEST:  Overbloom: ☀172
     */
    private val overbloomPattern by patternGroup.pattern(
        "overbloom",
        " *Overbloom: ☀(?<value>[\\d,.]+)(?: .*)?",
    )
    private var display: Renderable? = null

    @HandleEvent
    fun onWorldChange() {
        display = null
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onWidgetUpdate(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.STATS)) return
        val compact = config.overbloomDisplay.get() == DisplayFormat.COMPACT
        overbloomPattern.matchAllComponents(event.widget.lines) {
            val value = group("value").formatInt()

            display = Renderable.text {
                if (compact) append("§e☀ OB§7: ") else append("§e☀ Overbloom§7: ")
                append("§f$value")
            }
            return
        }
    }

    @HandleEvent(GuiRenderEvent.GuiOverlayRenderEvent::class)
    fun onGuiRenderOverlay() {
        if (!isEnabled()) return
        val display = display ?: return
        config.overbloomDisplayPosition.renderRenderable(display, posLabel = "Overbloom")
    }

    private fun isEnabled() = GardenApi.inGarden() && config.overbloomDisplay.get() != DisplayFormat.DISABLED && !GardenApi.hideExtraGuis()

    enum class DisplayFormat(private val displayName: String) {
        DISABLED("Disabled"),
        COMPACT("Compact"),
        FULL("Full"),
        ;

        override fun toString() = displayName
    }
}
