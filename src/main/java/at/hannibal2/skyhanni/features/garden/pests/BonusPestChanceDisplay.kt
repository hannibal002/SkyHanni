package at.hannibal2.hanni.features.garden.pests

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.model.TabWidget
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.events.WidgetUpdateEvent
import at.hannibal2.hanni.events.minecraft.WorldChangeEvent
import at.hannibal2.hanni.features.garden.GardenApi
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.NumberUtil.formatInt
import at.hannibal2.hanni.utils.RegexUtils.groupOrNull
import at.hannibal2.hanni.utils.RegexUtils.matchMatcher
import at.hannibal2.hanni.utils.RenderUtils.renderString
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@HanniModule
object BonusPestChanceDisplay {

    private val config get() = PestApi.config

    private val patternGroup = RepoPattern.group("garden.bonuspestchance")

    /**
     * REGEX-TEST:  §r§7§mBonus Pest Chance: ൠ70
     * REGEX-TEST:  Bonus Pest Chance: §r§2ൠ70
     */
    private val bonusPestChancePattern by patternGroup.pattern(
        "widget",
        "\\s+(?:§.)*?(?<disabled>§m)?Bonus Pest Chance: (?:§.)*ൠ(?<amount>[\\d,.]+)",
    )
    private var display: String? = null

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        display = null
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onWidgetUpdate(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.STATS)) return
        event.widget.lines.forEach { line ->
            bonusPestChancePattern.matchMatcher(line) {
                val disabled = groupOrNull("disabled") != null
                val amount = group("amount").formatInt()

                display = buildString {
                    append("§2ൠ Bonus Pest Chance ")
                    if (disabled) append("§c§m") else append("§f")
                    append("$amount%")
                    if (disabled) append("§r §cDISABLED")
                }
                return
            }
        }
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        config.pestChanceDisplayPosition.renderString(display, posLabel = "Bonus Pest Chance")
    }

    private fun isEnabled() = GardenApi.inGarden() && config.pestChanceDisplay && !GardenApi.hideExtraGuis()
}
