package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.utils.tracker.GardenSession
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class GardenTrackerUptimeConfig {
    @Expose
    @ConfigOption(
        name = "Uptime Activities",
        desc = "Choose what activities to include in total uptime." +
            "\nActivities not in this list will still be tracked in the background."
    )
    @ConfigEditorDraggableList
    val types: Property<MutableList<GardenSession>> =
        Property.of(mutableListOf(GardenSession.CROP, GardenSession.PEST, GardenSession.VISITOR))

    @Expose
    @ConfigOption(
        name = "AFK timeout",
        desc = "Pause Garden Trackers if you have not farmed this amount of time."
    )
    @ConfigEditorSlider(minValue = 15f, maxValue = 900f, minStep = 15f)
    var afkTimeout: Int = 300
}
