package at.hannibal2.skyhanni.config.features.gui.customscoreboard

import at.hannibal2.skyhanni.utils.RenderUtils
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class AlignmentConfig {
    @Expose
    @ConfigOption(name = "Horizontal Alignment", desc = "Alignment for the scoreboard on the horizontal axis.")
    @ConfigEditorDropdown
    var horizontalAlignment: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.RIGHT

    @Expose
    @ConfigOption(name = "Vertical Alignment", desc = "Alignment for the scoreboard on the vertical axis.")
    @ConfigEditorDropdown
    var verticalAlignment: RenderUtils.VerticalAlignment = RenderUtils.VerticalAlignment.CENTER

    @Expose
    @ConfigOption(name = "Margin", desc = "Space between the border of your screen and the scoreboard.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 50f, minStep = 1f)
    var margin: Int = 0
}
