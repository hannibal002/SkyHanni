package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

class DnaAnalyzerSolverConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Solves the DNA Analyzer.")
    @ConfigEditorBoolean
    @FeatureToggle
    @SearchTag("Analyser")
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Use Middle Click", desc = "Click on slots with middle click to speed up interactions.")
    @ConfigEditorBoolean
    var useMiddleClick: Boolean = true

    @Expose
    @ConfigOption(name = "Block Close Button", desc = "Blocks accidentally clicking the close button while in the puzzle.")
    @ConfigEditorBoolean
    var blockAccidentalClosing: Boolean = true

    @Expose
    @ConfigOption(name = "Hide tooltips", desc = "Hide the item tooltips inside the DNA Analyzer.")
    @ConfigEditorBoolean
    @SearchTag("Analyser")
    var hideTooltips: Boolean = true
}
