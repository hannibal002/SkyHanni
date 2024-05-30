package at.hannibal2.skyhanni.config.features.mining;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class ProfessorRobotConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Sends a clickable message in chat which searchs the Bazaar for the component you need. Only happens if you don't have the component in your inventory.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "I Have the Sack", desc = "Enable this for Professor Robot to use the Crystal Hollows Sack instead of the Bazaar.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean sack = false;
}
