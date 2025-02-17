package at.hannibal2.skyhanni.config.features.misc.frogmask

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class FrogMaskWarningConfig {

    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Displays a warning when foraging/being in a wrong region of the park while wearing a §5Frog Mask§7.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Cooldown", desc = "Change how much time needs to pass before you get warned again.")
    @ConfigEditorSlider(minValue = 5f, maxValue = 60f, minStep = 1f)
    var cooldown: Float = 30f

    @Expose
    @ConfigOption(name = "Warning Type", desc = "Change when you want to be warned.")
    @ConfigEditorDraggableList
    var warningType: List<WarningType> = ArrayList(listOf(WarningType.FORAGING))

    enum class WarningType(private val displayName: String) {
        BEING("When being in a wrong region."),
        FORAGING("When foraging in a wrong region.");

        override fun toString(): String {
            return displayName
        }
    }
}
