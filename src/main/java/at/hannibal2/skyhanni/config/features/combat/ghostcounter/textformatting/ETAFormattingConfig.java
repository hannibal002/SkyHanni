package at.hannibal2.skyhanni.config.features.combat.ghostcounter.textformatting;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorText;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class ETAFormattingConfig {
    @Expose
    @ConfigOption(name = "ETA to next level", desc = "ETA To Next Level Line.\n" +
        "§e%value% §7is replaced with one of the text below.")
    @ConfigEditorText
    public String base = "  &6ETA: &b%value%";

    @Expose
    @ConfigOption(name = "Maxed!", desc = "So you really maxed ghost bestiary ?")
    @ConfigEditorText
    public String maxed = "&c&lMAXED!";

    @Expose
    @ConfigOption(name = "No Data", desc = "Start killing some ghosts !")
    @ConfigEditorText
    public String noData = "&bN/A";

    @Expose
    @ConfigOption(name = "Progress", desc = "Text to show progress to next level.")
    @ConfigEditorText
    public String progress = "&b%value%";

    @Expose
    @ConfigOption(name = "Paused", desc = "Text displayed next to the time \n" +
        "when you are doing nothing for a given amount of seconds")
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
