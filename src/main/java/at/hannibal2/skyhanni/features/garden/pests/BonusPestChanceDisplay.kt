package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
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
    fun onWorldChange() {
        display = null
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onWidgetUpdate(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.STATS)) return
        val compact = config.pestChanceDisplay.get() == DisplayFormat.COMPACT
        event.widget.lines.forEach { line ->
            bonusPestChancePattern.matchMatcher(line) {
                val disabled = groupOrNull("disabled") != null
                val amount = group("amount").formatInt()

                display = buildString {
                    if (compact) append("§2ൠ BPC ") else append("§2ൠ Bonus Pest Chance ")
                    if (disabled) append("§c§m") else append("§f")
                    append("$amount%")
                    if (disabled && !compact) append("§r §cDISABLED")
                }
                return
            }
        }
    }

    @HandleEvent(GuiRenderEvent.GuiOverlayRenderEvent::class)
    fun onRenderOverlay() {
        if (!isEnabled()) return
        config.pestChanceDisplayPosition.renderString(display, posLabel = "Bonus Pest Chance")
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(118, "garden.pests.pestChanceDisplay") { entry ->
            ConfigUtils.migrateBooleanToEnum(entry, DisplayFormat.FULL, DisplayFormat.DISABLED)
        }
    }

    private fun isEnabled() = GardenApi.inGarden() && config.pestChanceDisplay.get() != DisplayFormat.DISABLED && !GardenApi.hideExtraGuis()

    enum class DisplayFormat(private val displayName: String) {
        DISABLED("Disabled"),
        COMPACT("Compact"),
        FULL("Full"),
        ;

        override fun toString() = displayName
    }
}
