package at.hannibal2.skyhanni.config.features.gui.customscoreboard;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class CustomLinesConfig {

    @Expose
    @ConfigOption(name = "Custom Line 1", desc = "Custom line 1")
    @ConfigEditorText
    public String customLine1 = "";
}
