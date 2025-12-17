package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
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
        name = "Show Pesthunter Bonus Fortune",
        desc = "Display the bonus fortune amount and time left from the bonus given by trading in pests at Pesthunter Phillip."
    )
    @ConfigEditorBoolean
    var showPestBonusFortune: Boolean = false

    @Expose
    @ConfigOption(
        name = "Bonus Fortune Expire Warning",
        desc = "Warn in chat when the pest fortune buff expires."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var bonusFortuneChat: Boolean = true

    @Expose
    @ConfigOption(
        name = "Bonus Fortune Expire Title",
        desc = "Send a title and sound when the pest fortune buff expires."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var bonusFortuneTitle: Boolean = false

    @Expose
    @ConfigOption(name = "Sound Settings", desc = "")
    @Accordion
    val sound: PestBuffWarningSoundConfig = PestBuffWarningSoundConfig()

    @Expose
    @ConfigLink(owner = FarmingFortuneConfig::class, field = "display")
    val position: Position = Position(5, -180)
}
