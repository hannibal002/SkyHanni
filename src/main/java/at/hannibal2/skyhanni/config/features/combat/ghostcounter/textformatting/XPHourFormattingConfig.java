package at.hannibal2.skyhanni.config.features.combat.ghostcounter.textformatting;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class XPHourFormattingConfig {

    @Expose
    @ConfigOption(name = "XP/h", desc = "XP per Hour line.\n" +
        "§e%value% §7is replaced with one of the text below.")
    @ConfigEditorText
    public String base = "  &6XP/h: &b%value%";

    @Expose
    @ConfigOption(name = "No Data", desc = "XP per Hour line.\n" +
        "§e%value% §7is replaced with estimated amount of Combat XP you gain per hour.")
    @ConfigEditorText
    public String noData = "&bN/A";

    @Expose
    @ConfigOption(name = "Paused", desc = "Text displayed next to the time when paused.")
    @ConfigEditorText
    public String paused = "&c(PAUSED)";
}

