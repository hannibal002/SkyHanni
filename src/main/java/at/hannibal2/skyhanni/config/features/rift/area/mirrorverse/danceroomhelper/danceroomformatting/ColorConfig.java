package at.hannibal2.skyhanni.config.features.rift.area.mirrorverse.danceroomhelper.danceroomformatting;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class ColorConfig {
    @Expose
    @ConfigOption(name = "Move", desc = "Colour for the Move instruction")
    @ConfigEditorText
    public String move = "&e";

    @Expose
    @ConfigOption(name = "Stand", desc = "Colour for the Stand instruction")
    @ConfigEditorText
    public String stand = "&e";

    @Expose
    @ConfigOption(name = "Sneak", desc = "Colour for the Sneak instruction")
    @ConfigEditorText
    public String sneak = "&5";

    @Expose
    @ConfigOption(name = "Jump", desc = "Colour for the Jump instruction")
    @ConfigEditorText
    public String jump = "&b";

    @Expose
    @ConfigOption(name = "Punch", desc = "Colour for the Punch instruction")
    @ConfigEditorText
    public String punch = "&d";

    @Expose
    @ConfigOption(name = "Countdown", desc = "Colour for the Countdown")
    @ConfigEditorText
    public String countdown = "&f";

    @Expose
    @ConfigOption(name = "Default", desc = "Fallback colour")
    @ConfigEditorText
    public String fallback = "&f";
}
