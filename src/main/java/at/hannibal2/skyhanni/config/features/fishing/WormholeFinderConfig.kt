package at.hannibal2.skyhanni.config.features.fishing

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class WormholeFinderConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Detect wormhole arrows and set a waypoint to the nearest wormhole.")
    @FeatureToggle
    @ConfigEditorBoolean
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Departure Alert", desc = "Show a title alert when a nearby wormhole departs.")
    @ConfigEditorBoolean
    var departureAlert: Boolean = true

}
