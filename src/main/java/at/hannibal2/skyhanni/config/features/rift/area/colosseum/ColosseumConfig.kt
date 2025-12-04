package at.hannibal2.skyhanni.config.features.rift.area.colosseum

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ColosseumConfig {
    @Expose
    @ConfigOption(
        name = "Highlight Blobbercysts",
        desc = "Highlight Blobbercysts in the Bacte fight.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightBlobbercysts: Boolean = true

    @Expose
    @ConfigOption(
        name = "Kill Zone Warning",
        desc = "Alert when you're about to die from being outside the arena during the Bacte fight.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var killZoneWarning: Boolean = true

    @Expose
    @ConfigOption(
        name = "Tentacle Waypoints",
        desc = "Show waypoints for tentacles with their HP in the Bacte fight.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var tentacleWaypoints: Boolean = true

    @Expose
    @ConfigOption(name = "Bacte Phase", desc = "Show the current phase of Bacte.")
    @ConfigEditorBoolean
    var bactePhaseDisplay: Boolean = false
}
