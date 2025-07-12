package at.hannibal2.skyhanni.config.features.mining.nucleus

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

class MetalDetectorConfig {

    @ConfigOption(name = "Credits", desc = "Most features here are from Coleweight or SoopyV2, huge thanks to them!")
    @ConfigEditorInfoText
    var notice: String = ""

    @Expose
    @ConfigOption(name = "Metal Detector Solver", desc = "Enables the solver for the metal detector.")
    @ConfigEditorBoolean
    @SearchTag("Mines of Divan mod jade")
    @FeatureToggle
    var metalDetectorSolver: Boolean = true

    @Expose
    @ConfigOption(name = "Show Time Taken", desc = "Shows how long it took you to find the treasure.")
    @ConfigEditorBoolean
    var showTimeTaken: Boolean = false

    @Expose
    @ConfigOption(name = "All Tools Alert", desc = "Alert when you have all the metal detector tools.")
    @ConfigEditorBoolean
    var metalDetectorAllToolsAlert: Boolean = true

    @Expose
    @ConfigOption(name = "Mute Metal Detector", desc = "Mute the metal detector sound.")
    @ConfigEditorBoolean
    var muteMetalDetectorSound: Boolean = false
}
