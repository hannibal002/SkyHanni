package at.hannibal2.skyhanni.config.features.inventory.experimentationtable

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ExperimentsAddonHelpersConfig {

    @Expose
    @ConfigOption(
        name = "Next Click Helper",
        desc = "Highlights the next slot to click in Chronomatron, and shows all items in Ultrasequencer."
    )
    @ConfigEditorBoolean
    var highlightNextClick: Boolean = false

    @Expose
    @ConfigOption(
        name = "Prevent Misclicks",
        desc = "Prevent clicking wrong colors in Chronomatron, and wrong slots in Ultrasequencer."
    )
    @ConfigEditorBoolean
    var preventMisclicks: Boolean = false

}
