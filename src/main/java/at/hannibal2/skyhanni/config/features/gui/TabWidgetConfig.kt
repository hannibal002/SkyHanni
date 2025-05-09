package at.hannibal2.skyhanni.config.features.gui

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.PositionList
//#if MC < 1.21
import at.hannibal2.skyhanni.features.gui.TabWidgetDisplay
//#endif
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

// todo 1.21 impl needed
class TabWidgetConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Enables the gui elements for the selected widgets.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @ConfigOption(
        name = "Not working Info",
        desc = "If the information isn't shown in the tablist it won't show anything. Use /widget to turn on the information you need."
    )
    @ConfigEditorInfoText
    var text1: String? = null

    @ConfigOption(
        name = "Enable Info",
        desc = "Drag only one new value at time into the list, since the default locations are all the same."
    )
    @ConfigEditorInfoText
    var text2: String? = null

    //#if MC < 1.21
    @Expose
    @ConfigOption(name = "Widgets", desc = "")
    @ConfigEditorDraggableList
    var display: MutableList<TabWidgetDisplay> = mutableListOf()

    @Expose
    @ConfigLink(owner = TabWidgetConfig::class, field = "enabled")
    var displayPositions: PositionList = PositionList(TabWidgetDisplay.entries.size)
    //#endif
}
