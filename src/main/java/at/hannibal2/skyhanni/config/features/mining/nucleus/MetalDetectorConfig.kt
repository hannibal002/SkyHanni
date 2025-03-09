package at.hannibal2.skyhanni.config.features.mining.nucleus

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class MetalDetectorConfig {
    @Expose
    @ConfigOption(name = "Metal Detector Solver", desc = "Enables the solver for the metal detector.")
    @ConfigEditorBoolean
    @FeatureToggle
    var metalDetectorSolver: Boolean = false

    @Expose
    @ConfigOption(name = "Metal Detector All Tools Alert", desc = "Alert when you have all the metal detector tools.")
    @ConfigEditorBoolean
    @FeatureToggle
    var metalDetectorAllToolsAlert: Boolean = false

    @Expose
    @ConfigOption(name = "Mute Metal Detector Sound", desc = "Mute the metal detector sound.")
    @ConfigEditorBoolean
    @FeatureToggle
    var muteMetalDetectorSound: Boolean = false
}
