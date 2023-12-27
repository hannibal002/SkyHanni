package at.hannibal2.skyhanni.config.features.gui.customscoreboard;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class InformationFilteringConfig {
    @Expose
    @ConfigOption(name = "Hide lines with no info", desc = "Hide lines that have no info to display, like hiding the party when not being in one.")
    @ConfigEditorBoolean
    public boolean hideEmptyLines = true;

    @Expose
    @ConfigOption(name = "Hide Info not relevant to location", desc = "Hide lines that are not relevant to the current location, like hiding copper while not in garden.")
    @ConfigEditorBoolean
    public boolean hideIrrelevantLines = true;

    @Expose
    @ConfigOption(name = "Hide consecutive empty lines", desc = "Hide lines that are empty and have an empty line above them.")
    @ConfigEditorBoolean
    public boolean hideConsecutiveEmptyLines = true;
}
