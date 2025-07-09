package at.hannibal2.skyhanni.config.features.inventory.experimentationtable

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

class ExperimentsAddonsConfig {

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
    @SearchTag("missclick")
    var preventMisclicks: Boolean = true

    @Expose
    @ConfigOption(
        name = "Max Clicks Alert",
        desc = "Display an alert when you reach the maximum clicks gained from Chronomatron or Ultrasequencer."
    )
    @ConfigEditorBoolean
    var maxSequenceAlert: Boolean = true

}
