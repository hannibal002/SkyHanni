package at.hannibal2.skyhanni.config.features.inventory.experimentationtable

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.features.inventory.experimentationtable.ExperimentMessages
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ExperimentsProfitTrackerConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Tracker for drops/XP you get from experiments.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Hide Messages",
        desc = "Change the messages to be hidden after completing Add-on/Main experiments."
    )
    @ConfigEditorDraggableList
    val hideMessages: MutableList<ExperimentMessages> = mutableListOf()

    @Expose
    @ConfigOption(name = "Time displayed", desc = "Time displayed after completing an experiment.")
    @ConfigEditorSlider(minValue = 5f, maxValue = 60f, minStep = 1f)
    var timeDisplayed: Int = 30

    @Expose
    @ConfigLink(owner = ExperimentsProfitTrackerConfig::class, field = "enabled")
    val position: Position = Position(20, 20)
}
