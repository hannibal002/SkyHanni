package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.features.garden.GardenPlotApi
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class PlotDisplayConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Display the current state of your plots in a display.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Statuses", desc = "Change which statuses are enabled, and the hierarchy of them.")
    @ConfigEditorDraggableList
    val displayedStatusTypes: MutableList<GardenPlotApi.PlotStatusType> = mutableListOf(
        GardenPlotApi.PlotStatusType.CURRENT,
        GardenPlotApi.PlotStatusType.PESTS,
        GardenPlotApi.PlotStatusType.SPRAYS,
        GardenPlotApi.PlotStatusType.LOCKED
    )

    @Expose
    @ConfigLink(owner = PlotDisplayConfig::class, field = "enabled")
    val displayPos: Position = Position(20, 20)
}
