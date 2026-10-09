package at.hannibal2.skyhanni.config.features.fishing

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.utils.EnumUtils.toFormattedName
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class WormholeFinderConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Detect wormhole arrows and set a waypoint to the nearest wormhole.")
    @FeatureToggle
    @ConfigEditorBoolean
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Guide Mode", desc = "Choose how to draw the line to detected wormholes.")
    @ConfigEditorDropdown
    var lineMode: LineMode = LineMode.NAVIGATION

    enum class LineMode {
        OFF,
        DIRECT,
        NAVIGATION,
        ;

        private val displayName = toFormattedName()
        override fun toString(): String = displayName
    }

    @Expose
    @ConfigOption(name = "Departure Alert", desc = "Show a title alert when a nearby wormhole departs.")
    @ConfigEditorBoolean
    var departureAlert: Boolean = true

}
