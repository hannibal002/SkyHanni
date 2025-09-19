package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.utils.tracker.GardenSession
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class GardenTrackerUptimeConfig {
    @Expose
    @ConfigOption(name = "Uptime Activities", desc = "Choose what activities to include in total uptime.")
    @ConfigEditorDraggableList
    val types: Property<MutableList<GardenSession>> =
        Property.of(mutableListOf(GardenSession.CROP, GardenSession.PEST, GardenSession.VISITOR))
}
