package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object OverbloomDisplay {

    private val config get() = GardenApi.config

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onGuiRenderOverlay() {
        if (config.overbloomDisplayFormat == DisplayFormat.DISABLED) return
        if (GardenApi.hideExtraGuis()) return

        SkyblockStat.OVERBLOOM.renderFormattedDisplay(config.overbloomDisplayPosition) {
            if (config.overbloomDisplayFormat == DisplayFormat.COMPACT) {
                it.replace("Overbloom", "OB")
            } else it
        }
    }

    enum class DisplayFormat(private val displayName: String) {
        ENABLED("Enabled"),
        COMPACT("Compact"),
        DISABLED("Disabled"),
        ;

        override fun toString() = displayName
    }
}
