package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class MPDisplayConfig {
    @Expose
    @ConfigOption(name = "Enable MP Display", desc = "Show Magical Power as stack size.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Colored", desc = "Whether to make the numbers colored.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean colored = false;
}
