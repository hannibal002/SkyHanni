package at.hannibal2.skyhanni.config.features.gui.customscoreboard

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ArrowConfig {
    @Expose
    @ConfigOption(name = "Arrow Amount Display", desc = "Determine how the arrow amount is displayed.")
    @ConfigEditorDropdown
    var arrowAmountDisplay: ArrowAmountDisplay = ArrowAmountDisplay.NUMBER

    enum class ArrowAmountDisplay(private val displayName: String) {
        NUMBER("Number"),
        PERCENTAGE("Percentage"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Color Arrow Amount", desc = "Color the arrow amount based on the percentage.")
    @ConfigEditorBoolean
    var colorArrowAmount: Boolean = false
}
