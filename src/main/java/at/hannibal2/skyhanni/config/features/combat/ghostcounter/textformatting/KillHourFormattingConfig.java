package at.hannibal2.skyhanni.config.features.combat.ghostcounter.textformatting;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class KillHourFormattingConfig {
    @Expose
    @ConfigOption(name = "Kills/h", desc = "Kills per Hour line.\n" +
        "§e%value% §7is replaced with the estimated kills per hour.")
    @ConfigEditorText
    public String base = "  &6Kill/h: &b%value%";

    @Expose
    @ConfigOption(name = "No Data", desc = "Text to show when there is no Kills per Hour data.")
    @ConfigEditorText
    public String noData = "&bN/A";

    @Expose
    @ConfigOption(name = "Paused", desc = "Text displayed next to the time when paused.")
    @ConfigEditorText
    public String paused = "&c(PAUSED)";
}

