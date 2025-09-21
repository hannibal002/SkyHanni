package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.misc.tracker.timed.TimedGardenIndividualTrackerConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class GardenBpsTrackerConfig {
    @Expose
    @ConfigOption(name = "Enable Tracker", desc = "Track crop block breaks in garden.")
    @ConfigEditorBoolean
    @FeatureToggle
    var showDisplay: Boolean = false

    @Expose
    @ConfigOption(name = "Stats List", desc = "Drag text to change what displays in the summary card.")
    @ConfigEditorDraggableList
    val uptimeDisplayText: Property<MutableList<GardenUptimeDisplayText>> = Property.of(GardenUptimeDisplayText.defaultValues)

    @Expose
    @ConfigOption(
        name = "Tracker Settings",
        desc = ""
    )
    @Accordion
    val perTrackerConfig: TimedGardenIndividualTrackerConfig = TimedGardenIndividualTrackerConfig()

    enum class GardenUptimeDisplayText(private val str: String) {
        TITLE("Crop Break Tracker"),
        BPS("Blocks/Second: 17.11"),
        BLOCKS_BROKEN("Blocks Broken: 17,912"),
        ;

        override fun toString(): String {
            return str
        }

        companion object {
            @Suppress("StorageNeedsExpose")
            val defaultValues: MutableList<GardenUptimeDisplayText> = mutableListOf(
                TITLE,
                BPS,
                BLOCKS_BROKEN
            )
        }
    }

    @Expose
    @ConfigLink(owner = GardenBpsTrackerConfig::class, field = "showDisplay")
    val pos: Position = Position(5, -180, false, true)
}
