package at.hannibal2.skyhanni.config.features.gui.customscoreboard

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class InformationFilteringConfig {
    @Expose
    @ConfigOption(
        name = "Hide lines with no info",
        desc = "Hide lines that have no info to display, like hiding the party when not being in one."
    )
    @ConfigEditorBoolean
    var hideEmptyLines: Boolean = true

    @Expose
    @ConfigOption(
        name = "Hide consecutive empty lines",
        desc = "Hide lines that are empty and have an empty line above them."
    )
    @ConfigEditorBoolean
    var hideConsecutiveEmptyLines: Boolean = true

    @Expose
    @ConfigOption(
        name = "Hide empty lines at top/bottom",
        desc = "Hide empty lines at the top or bottom of the scoreboard."
    )
    @ConfigEditorBoolean
    var hideEmptyLinesAtTopAndBottom: Boolean = true

    @Expose
    @ConfigOption(
        name = "Hide non relevant info",
        desc = "Hide lines that are not relevant to the current location.\n" +
            "§cIt's generally not recommended to turn this off."
    )
    @ConfigEditorBoolean
    var hideIrrelevantLines: Boolean = true
}
