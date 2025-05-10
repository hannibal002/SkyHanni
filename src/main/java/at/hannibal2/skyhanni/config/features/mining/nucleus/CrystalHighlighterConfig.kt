package at.hannibal2.skyhanni.config.features.mining.nucleus

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CrystalHighlighterConfig {
    @Expose
    @ConfigOption(
        name = "Highlight Nucleus Barriers",
        desc = "Draw visible bounding boxes around the Crystal Nucleus crystal barrier blocks."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Highlight Design", desc = "Change the design of the highlighted boxes.")
    @ConfigEditorDropdown
    var boxStyle: BoundingBoxType = BoundingBoxType.FILLED

    enum class BoundingBoxType(private val displayName: String) {
        FILLED("Filled"),
        OUTLINE("Outline");

        override fun toString() = displayName
    }

    @Expose
    @Accordion
    @ConfigOption(name = "Highlight Colors", desc = "")
    val colors: CrystalHighlighterColorConfig = CrystalHighlighterColorConfig()

    @Expose
    @ConfigOption(name = "Only Show During Hoppity's", desc = "Only show the highlighted boxes during Hoppity's Hunt.")
    @ConfigEditorBoolean
    var onlyDuringHoppity: Boolean = false
}
