package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class PersonOfInterestConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Enable notification when a player from the list is in your lobby.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Players List", desc = "Players list you want to be notified for.\n§cCase sensitive, separated by comma.")
    @ConfigEditorText
    public String playersList = "hypixel";
}
