package at.hannibal2.skyhanni.config.features.garden.pests

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class StereoHarmonyConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Show a display of what pest is being boosted by your vinyl.")
    @ConfigEditorBoolean
    @FeatureToggle
    var displayEnabled: Boolean = true

    @Expose
    @ConfigOption(name = "Always Show", desc = "Show the display even while not farming.")
    @ConfigEditorBoolean
    var alwaysShow: Boolean = false

    @Expose
    @ConfigOption(name = "Show Pest Head", desc = "Show the head of the pest being boosted.")
    @ConfigEditorBoolean
    val showHead: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Show Crop Icon", desc = "Show the icon of the crops dropped by the pests being boosted.")
    @ConfigEditorBoolean
    val showCrop: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Hide when None", desc = "Hide when no vinyl selected.")
    @ConfigEditorBoolean
    var hideWhenNone: Boolean = false

    @Expose
    @ConfigOption(name = "Replace Menu Icons", desc = "Show pest crops instead of music discs in the selector menu.")
    @ConfigEditorBoolean
    var replaceMenuIcons: Boolean = true

    @Expose
    @ConfigLink(owner = StereoHarmonyConfig::class, field = "displayEnabled")
    val position: Position = Position(205, 55, 1f)
}
