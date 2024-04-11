package at.hannibal2.skyhanni.config.features.mining;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class AreaWallsConfig {

    @Expose
    @ConfigOption(name = "Area Walls", desc = "Show walls between the main areas of the Crystal Hollows.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "In Nucleus", desc = "Shows the walls also when inside the Nucleus.")
    @ConfigEditorBoolean
    public boolean nucleus = false;
}
