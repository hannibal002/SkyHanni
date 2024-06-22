package at.hannibal2.skyhanni.config.features.combat.ghostcounter.textformatting;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class ETAFormattingConfig {
    @Expose
    @ConfigOption(name = "ETA to Next Level", desc = "ETA To Next Level line.\n" +
        "§e%value% §7is replaced with one of the text below.")
    @ConfigEditorText
    public String base = "  &6ETA: &b%value%";

    @Expose
    @ConfigOption(name = "Maxed!", desc = "Text to show when ghost bestiary is maxed.")
    @ConfigEditorText
    public String maxed = "&c&lMAXED!";

    @Expose
    @ConfigOption(name = "No Data", desc = "Text to show when there is no ETA.")
    @ConfigEditorText
    public String noData = "&bN/A";

    @Expose
    @ConfigOption(name = "Progress", desc = "Text to show progress to the next level.")
    @ConfigEditorText
    public String progress = "&b%value%";

    @Expose
    @ConfigOption(name = "Paused", desc = "Text displayed next to the time when paused.")
    @ConfigEditorText
    public String paused = "&c(PAUSED)";

    @Expose
    @ConfigOption(name = "Time", desc = "§e%days% §7is replaced with days remaining.\n" +
        "§e%hours% §7is replaced with hours remaining.\n" +
        "§e%minutes% §7is replaced with minutes remaining.\n" +
        "§e%seconds% §7is replaced with seconds remaining.")
    @ConfigEditorText
    public String time = "&6%days%%hours%%minutes%%seconds%";
}
