package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class GardenLevelConfig {
    @Expose
    @ConfigOption(name = "Display", desc = "Show the current Garden level and progress to the next level.")
    @ConfigEditorBoolean
    @FeatureToggle
    var display: Boolean = false

    @Expose
    @ConfigOption(name = "Overflow", desc = "Enable overflow Garden levels")
    @ConfigEditorBoolean
    @FeatureToggle
    var overflow: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Overflow Chat", desc = "Enable overflow Garden level up messages in chat.")
    @ConfigEditorBoolean
    @FeatureToggle
    var overflowChat: Boolean = true

    @Expose
    @ConfigLink(owner = GardenLevelConfig::class, field = "display")
    var pos: Position = Position(390, 40)
}
