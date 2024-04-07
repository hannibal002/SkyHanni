package at.hannibal2.skyhanni.config.features.crimsonisle;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorColour;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class MatriarchHelperConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Enable features around Matriarch helper.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Highlight", desc = "Highlight the pearls in a color of your choosing")
    @ConfigEditorBoolean
    public boolean highlight = true;

    @Expose
    @ConfigOption(name = "Highlight Color", desc = "Color of the line.")
    @ConfigEditorColour
    public String highlightColor = "0:200:85:255:85";

    @Expose
    @ConfigOption(name = "Draw Line", desc = "Draw Line to the lowest Heavy Pearl")
    @ConfigEditorBoolean
    public boolean line = true;

    @Expose
    @ConfigOption(name = "Line Color", desc = "Color of the line.")
    @ConfigEditorColour
    public String lineColor = "0:101:201:39:255";
}
