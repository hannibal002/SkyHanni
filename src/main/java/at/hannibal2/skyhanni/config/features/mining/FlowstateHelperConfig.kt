package at.hannibal2.skyhanni.config.features.mining

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.features.mining.FlowstateElements
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

class FlowstateHelperConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Shows stats for the Flowstate enchantment on Mining Tools.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Appearance", desc = "Drag text to change the appearance.")
    @ConfigEditorDraggableList
    val appearance: MutableList<FlowstateElements> = FlowstateElements.defaultOption.toMutableList()

    @Expose
    @ConfigOption(name = "Dynamic Color", desc = "Makes the timer's color dynamic.")
    @ConfigEditorBoolean
    var colorfulTimer: Boolean = false

    @Expose
    @ConfigOption(name = "Auto Hide", desc = "Automatically hides the GUI after being idle, in seconds.")
    @SearchTag("autohide")
    @ConfigEditorSlider(minValue = -1f, maxValue = 30f, minStep = 1f)
    var autoHide: Int = 10

    @Expose
    @ConfigLink(owner = FlowstateHelperConfig::class, field = "enabled")
    val position: Position = Position(-110, 9)
}
