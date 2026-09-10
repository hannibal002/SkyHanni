package at.hannibal2.skyhanni.config.features.foraging

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SafariChecklistConfig {

    @Expose
    @ConfigOption(
        name = "Shard Checklist",
        desc = "Displays the Critter Safari shards collected during the current run.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var runShardChecklist: Boolean = true

    @Expose
    @ConfigOption(
        name = "Checklist Display",
        desc = "Select which Critter Safari shard checklist biomes to show.",
    )
    @ConfigEditorDropdown
    var runShardChecklistDisplay: ChecklistDisplay = ChecklistDisplay.ALL

    @Expose
    @ConfigOption(
        name = "Hide Collected Shards",
        desc = "Hide shards already collected during the current Critter Safari run.",
    )
    @ConfigEditorBoolean
    var hideCollectedRunShards: Boolean = false

    @Expose
    @ConfigLink(owner = SafariChecklistConfig::class, field = "runShardChecklist")
    val runShardChecklistPosition: Position = Position(80, 100)

    enum class ChecklistDisplay(private val displayName: String) {
        ALL("All"),
        CURRENT_ON_TOP("Current on Top"),
        ONLY_CURRENT("Only Current"),
        ;

        override fun toString() = displayName
    }
}
