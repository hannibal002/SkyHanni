package at.hannibal2.skyhanni.config.features.misc.frogmask

import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class FrogMaskFeaturesConfig {

    @Expose
    @ConfigOption(
        name = "Frog Mask Display",
        desc = "Displays information about the active §2Frog Mask§7 region. §eRequires a Frog Mask to be worn.",
    )
    @ConfigEditorDropdown
    var display: FrogMaskCondition = FrogMaskCondition.DISABLED

    enum class FrogMaskCondition(private val displayName: String) {
        DISABLED("Off"),
        ALWAYS("Always"),
        PARK("In The Park")
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Frog Mask Warning", desc = "")
    @Accordion
    val warning: FrogMaskWarningConfig = FrogMaskWarningConfig()

    @Expose
    @ConfigLink(owner = FrogMaskFeaturesConfig::class, field = "display")
    val position: Position = Position(25, 25)

}
