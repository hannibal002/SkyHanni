package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.SkyblockStat
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

        SkyblockStat.BONUS_PEST_CHANCE.renderFormattedDisplay(config.pestChanceDisplayPosition) {
            var it = it // yes

            val compact = config.pestChanceDisplay == DisplayFormat.COMPACT
            val disabled = it.contains("§m")

            it = it.plus("%") // add %

            if (compact)
                it = it.replace("Bonus Pest Chance", "BPC") // shorten name

            if (disabled) {
                it = it.replace("§f", "§c§m") // strikethrough
                if (!compact) it = it.plus("§r §cDISABLED") // add disabled text if no compact
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
