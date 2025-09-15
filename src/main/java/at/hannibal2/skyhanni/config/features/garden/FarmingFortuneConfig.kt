package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGui
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

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

    @Expose
    @ConfigOption(
        name = "Show Pest Fortune Buff",
        desc = "Display the Pest Fortune Buff amount and time left."
    )
    @ConfigEditorBoolean
    var pestBuff: Boolean = false

    @Expose
    @ConfigOption(
        name = "Pest Buff Expire Warning",
        desc = "Warn in chat when the pest fortune buff expires."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var pestBuffWarning: Boolean = true

    @Expose
    @ConfigOption(
        name = "Pest Buff Expire Title",
        desc = "Send a title and sound when the pest fortune buff expires."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var pestBuffTitle: Boolean = false

    @Expose
    @ConfigOption(name = "Sound Settings", desc = "")
    @Accordion
    val sound: PestBuffWarningSoundConfig = PestBuffWarningSoundConfig()

    @ConfigOption(
        name = "Farming Fortune Guide",
        desc = "Open a guide that breaks down your Farming Fortune.\n§eCommand: /ff"
    )
    @ConfigEditorButton(buttonText = "Open")
    val open: Runnable = Runnable(FFGuideGui::onCommand)

    @Expose
    @ConfigLink(owner = FarmingFortuneConfig::class, field = "display")
    val position: Position = Position(5, -180)
}
