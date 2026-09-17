package at.hannibal2.skyhanni.config.features.foraging

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class SafariChecklistConfig {

    @Expose
    @ConfigOption(
        name = "Shard Checklist",
        desc = "Displays the Critter Safari unique shards caught during the current run.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(
        name = "Checklist Display",
        desc = "Select which Critter Safari shard checklist biomes to show.",
    )
    @ConfigEditorDropdown
    val runDisplay: Property<ChecklistDisplay> = Property.of(ChecklistDisplay.ONLY_CURRENT)

    enum class ChecklistDisplay(private val displayName: String) {
        ALL("All"),
        CURRENT_ON_TOP("Current on Top"),
        ONLY_CURRENT("Only Current"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(
        name = "Hide Caught Shards",
        desc = "Hide shards already caught during the current Critter Safari run.",
    )
    @ConfigEditorBoolean
    val hideCaught: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(
        name = "Show Shard Icons",
        desc = "Display an item icon next to each shard in the checklist.",
    )
    @ConfigEditorBoolean
    val showIcons: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigLink(owner = SafariChecklistConfig::class, field = "enabled")
    val position: Position = Position(80, 100)

}
