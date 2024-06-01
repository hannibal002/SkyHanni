package at.hannibal2.skyhanni.config.features.gui.customscoreboard;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class MayorConfig {
    @Expose
    @ConfigOption(name = "Show Mayor Perks", desc = "Show the perks of the current mayor.")
    @ConfigEditorBoolean
    public boolean showMayorPerks = true;

    @Expose
    @ConfigOption(name = "Show Time until next mayor", desc = "Show the time until the next mayor is elected.")
    @ConfigEditorBoolean
    public boolean showTimeTillNextMayor = true;
}
