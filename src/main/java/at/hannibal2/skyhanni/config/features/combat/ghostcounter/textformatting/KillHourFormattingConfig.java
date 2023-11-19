package at.hannibal2.skyhanni.config.features.combat.ghostcounter.textformatting;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorText;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class KillHourFormattingConfig {
    @Expose
    @ConfigOption(name = "Kill/h", desc = "Kill Per Hour line.\n§e%value% §7is replaced with\nEstimated kills per hour you get.")
    @ConfigEditorText
    public String base = "  &6Kill/h: &b%value%";

    @Expose
    @ConfigOption(name = "No Data", desc = "Start killing some ghosts !")
    @ConfigEditorText
    public String noData = "&bN/A";

    @Expose
    @ConfigOption(name = "Paused", desc = "Text displayed next to the time \n" +
        "when you are doing nothing for a given amount of seconds")
    @ConfigEditorText
    public String paused = "&c(PAUSED)";
}

