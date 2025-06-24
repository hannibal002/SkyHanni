package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.utils.LorenzColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import java.util.*

class PlotMenuHighlightingConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Highlight plots based on their status.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Statuses", desc = "Change which statuses are enabled, and the hierarchy of them.")
    @ConfigEditorDraggableList
    val deskPlotStatusTypes: MutableList<PlotStatusType> = mutableListOf(
        PlotStatusType.CURRENT,
        PlotStatusType.PESTS,
        PlotStatusType.SPRAYS,
        PlotStatusType.LOCKED
    )

    enum class PlotStatusType(private val displayName: String, val highlightColor: LorenzColor) {
        PESTS("§cPests", LorenzColor.RED),
        SPRAYS("§6Sprays", LorenzColor.GOLD),
        LOCKED("§7Locked", LorenzColor.DARK_GRAY),
        CURRENT("§aCurrent plot", LorenzColor.GREEN),
        PASTING("§ePasting", LorenzColor.YELLOW),
        ;

        override fun toString() = displayName
    }
}
