package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConfigUtils

@SkyHanniModule
object BonusPestChanceDisplay {

    private val config get() = PestApi.config

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onGuiRenderOverlay() {
        if (config.pestChanceDisplay == DisplayFormat.DISABLED) return
        if (GardenApi.hideExtraGuis()) return

    /**
     * WRAPPED-REGEX-TEST: " Bonus Pest Chance: 70"
     * WRAPPED-REGEX-TEST: " Bonus Pest Chance: 100"
     */
    private val bonusPestChancePattern by patternGroup.pattern(
        "widget-no-color",
        "\\s+Bonus Pest Chance: ${SkyblockStat.BONUS_PEST_CHANCE.hypixelIcon}(?<amount>[\\d,.]+)",
    )
    private var display: Renderable? = null

            val compact = config.pestChanceDisplay == DisplayFormat.COMPACT
            val disabled = it.contains("§m")

            it = it.plus("%") // add %

            if (compact)
                it = it.replace("Bonus Pest Chance", "BPC") // shorten name

            display = Renderable.text {
                if (compact) append("§2 BPC ") else append("§2 Bonus Pest Chance ")
                if (disabled) append("§c§m") else append("§f")
                append("$amount%")
                if (disabled && !compact) append("§r §cDISABLED")
            }

            it // return modified text
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(118, "garden.pests.pestChanceDisplay") { entry ->
            ConfigUtils.migrateBooleanToEnum(entry, DisplayFormat.FULL, DisplayFormat.DISABLED)
        }
    }

    enum class DisplayFormat(private val displayName: String) {
        FULL("Enabled"),
        COMPACT("Compact"),
        DISABLED("Disabled"),
        ;

        override fun toString() = displayName
    }
}
