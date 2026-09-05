package at.hannibal2.skyhanni.config.features.garden.pests

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SprayDisplayConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Show the active spray and duration for your current plot.")
    @ConfigEditorBoolean
    @FeatureToggle
    var displayEnabled: Boolean = true

    @Expose
    @ConfigOption(name = "Show If Not Sprayed", desc = "Also show if current plot is not sprayed.")
    @ConfigEditorBoolean
    @FeatureToggle
    var showNotSprayed: Boolean = false

    @Expose
    @ConfigOption(name = "Hide when in Greenhouse", desc = "Disables Spray Display in Greenhouse.")
    @ConfigEditorBoolean
    var hideInGreenhouse: Boolean = true

    @Expose
    @ConfigLink(owner = SprayDisplayConfig::class, field = "displayEnabled")
    val displayPosition: Position = Position(390, 75)
}
