package at.hannibal2.skyhanni.config.features.misc.frogmask

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class FrogMaskFeaturesConfig {
    @Expose
    @ConfigOption(name = "Frog Mask Display", desc = "Displays information about the §5Frog Mask§7.")
    @ConfigEditorBoolean
    @FeatureToggle
    var display: Boolean = false

    @Expose
    @ConfigOption(name = "Frog Mask Warning", desc = "")
    @Accordion
    val warning: FrogMaskWarningConfig = FrogMaskWarningConfig()

    @Expose
    @ConfigLink(owner = FrogMaskFeaturesConfig::class, field = "display")
    val position: Position = Position(25, 25)
}
