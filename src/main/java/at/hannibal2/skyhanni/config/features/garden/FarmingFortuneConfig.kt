package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
//#if TODO
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGui
//#endif
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

// todo 1.21 impl needed
class FarmingFortuneConfig {
    @Expose
    @ConfigOption(
        name = "FF Display",
        desc = "Display the true Farming Fortune for the current crop, including all crop-specific and hidden bonuses."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var display: Boolean = false

    @Expose
    @ConfigOption(name = "Compact Format", desc = "Compact the farming fortune display.")
    @ConfigEditorBoolean
    var compactFormat: Boolean = false

    @Expose
    @ConfigOption(name = "Hide Missing Fortune Warnings", desc = "Hide missing fortune warnings from the display.")
    @ConfigEditorBoolean
    var hideMissingFortuneWarnings: Boolean = false

    //#if TODO
    @ConfigOption(
        name = "Farming Fortune Guide",
        desc = "Open a guide that breaks down your Farming Fortune.\n§eCommand: /ff"
    )
    @ConfigEditorButton(buttonText = "Open")
    var open: Runnable = Runnable(FFGuideGui::onCommand)
    //#endif

    @Expose
    @ConfigLink(owner = FarmingFortuneConfig::class, field = "display")
    var position: Position = Position(5, -180)
}
