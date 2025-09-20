package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.misc.tracker.IndividualTrackerConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.timed.TimedIndividualItemTrackerConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.timed.TimedIndividualTrackerConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property
import java.util.Arrays

class GardenBpsTrackerConfig {
    @Expose
    @ConfigOption(name = "Enable Tracker", desc = "Track garden uptime.")
    @ConfigEditorBoolean
    @FeatureToggle
    var showDisplay: Boolean = true

    @Expose
    @ConfigOption(name = "Stats List", desc = "Drag text to change what displays in the summary card.")
    @ConfigEditorDraggableList
    var uptimeDisplayText: Property<List<GardenUptimeDisplayText>> = Property.of(
        ArrayList(
            GardenUptimeDisplayText.defaultValues
        )
    )

    @Expose
    @ConfigOption(
        name = "Tracker Settings",
        desc = ""
    )
    @Accordion
    val perTrackerConfig: TimedIndividualTrackerConfig = TimedIndividualTrackerConfig()

    enum class GardenUptimeDisplayText(private val str: String) {
        TITLE("Garden Uptime"),
        BPS("Blocks/Second: 17.11"),
        BLOCKS_BROKEN("Blocks Broken: 17,912"),
        ;

        override fun toString(): String {
            return str
        }

        companion object {
            @Suppress("StorageNeedsExpose")
            val defaultValues: List<GardenUptimeDisplayText> = Arrays.asList(
                TITLE,
                BPS,
                BLOCKS_BROKEN
            )
        }
    }

    @Expose
    @ConfigLink(owner = GardenBpsTrackerConfig::class, field = "showDisplay")
    var pos: Position = Position(5, -180, false, true)
}
