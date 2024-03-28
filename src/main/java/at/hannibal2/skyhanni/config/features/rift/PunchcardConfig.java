package at.hannibal2.skyhanni.config.features.rift;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorColour;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class PunchcardConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Highlights unpunched players in the Rift.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Color", desc = "Color used for highlighting.")
    @ConfigEditorColour
    public String color = "0:163:122:11:143";
}
