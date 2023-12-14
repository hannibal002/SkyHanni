package at.hannibal2.skyhanni.config.features.gui.customscoreboard;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class MayorConfig {
    @Expose
    @ConfigOption(name = "Show Mayor Perks", desc = "Show the perks of the current mayor.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean showMayorPerks = true;

    @Expose
    @ConfigOption(name = "Show Time till next mayor", desc = "Show the time till the next mayor is elected.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean showTimeTillNextMayor = true;
}
