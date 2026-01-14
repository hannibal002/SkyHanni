package at.hannibal2.skyhanni.config.features.garden.pests

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class MantidDisplayConfig {

    @Expose
    @FeatureToggle
    @ConfigOption(name = "Enabled", desc = "Enables the display")
    @ConfigEditorBoolean
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "When to show", desc = "When to show this display")
    @ConfigEditorDraggableList
    val whenToShow: MutableList<WhenShowDisplay> = mutableListOf(WhenShowDisplay.MANTID)

    enum class WhenShowDisplay(val displayName: String) {
        ALWAYS("Always on Garden"),
        ARMOR("Wearing farming Armor"),
        MANTID("Using Mantid Reforge"),
        TOOL("Holding farming tool"),
        VACUUM("Holding vacuum"),
        ;

        override fun toString(): String = displayName
    }

    @Expose
    @ConfigOption(name = "Group Similar Expiry", desc = "Group pests that expire within this many seconds together.")
    @ConfigEditorSlider(minValue = 0.0F, maxValue = 120F, minStep = 5f)
    var groupSimilarExpire: Int = 30

    @Expose
    @ConfigLink(owner = MantidDisplayConfig::class, field = "enabled")
    val pos: Position = Position(200, 50)
}
