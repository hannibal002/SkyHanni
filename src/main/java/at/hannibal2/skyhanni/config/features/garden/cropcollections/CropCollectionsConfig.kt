package at.hannibal2.skyhanni.config.features.garden.cropcollections

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.misc.tracker.timed.TimedGardenIndividualTrackerConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import java.util.Arrays

class CropCollectionsConfig {
    @Expose
    @ConfigOption(
        name = "Collection Display",
        desc = """Show the progress and ETA until the next crop milestone is reached and the current crops/minute value.
§eRequires a tool with either a counter or Cultivating enchantment for full accuracy."""
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var collectionDisplay: Boolean = true

    @Expose
    @ConfigLink(owner = CropCollectionsConfig::class, field = "collectionDisplay")
    var collectionDisplayPos: Position = Position(-400, -200, false, true)

    @Expose
    @ConfigOption(
        name = "Crop Collection Display List",
        desc = "Drag text to change what displays in the summary card."
    )
    @ConfigEditorDraggableList
    var statDisplayList: List<CropCollectionDisplayText> =
        ArrayList(CropCollectionDisplayText.defaultCollectionDisplayList)

    @Expose
    @ConfigOption(
        name = "Tracker Settings",
        desc = ""
    )
    @Accordion
    val perTrackerConfig: TimedGardenIndividualTrackerConfig = TimedGardenIndividualTrackerConfig()

    enum class CropCollectionDisplayText(private val display: String) {
        TITLE("Melon Crop Collection"),
        ALL_TIME("All time: 27,898,115"),
        SESSION("Today: 18,211,121"),
        PER_HOUR("Per hour: 5,677,121"),
        BREAKDOWN("Collection Breakdown:"),
        FARMING("From Farming: 8,111,881"),
        BREAKING_CROPS("- Breaking Crops: 4,111,987"),
        MOOSHROOM_COW("- Mooshroom Cow: 123,812"),
        DICER("- Dicer Drops: 178,991"),
        PESTS("From Pests: 3,455,192"),
        PEST_RNG("- Pest Crop RNG: 211,192"),
        PEST_BASE("- Pest Base Drops: 129,128"),
        ;

        override fun toString(): String {
            return display
        }

        companion object {
            @Suppress("StorageNeedsExpose")
            val defaultCollectionDisplayList: List<CropCollectionDisplayText> = Arrays.asList(
                TITLE,
                ALL_TIME,
                SESSION,
                PER_HOUR
            )
        }
    }
}
