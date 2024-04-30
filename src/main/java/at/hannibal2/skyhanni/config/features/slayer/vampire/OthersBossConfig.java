package at.hannibal2.skyhanni.config.features.slayer.vampire;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class OthersBossConfig {

    @Expose
    @ConfigOption(name = "Highlight Others Boss", desc = "Highlight others players boss.\nYou need to hit them first.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean highlight = true;

    @Expose
    @ConfigOption(name = "Highlight Color", desc = "What color to highlight the boss in.")
    @ConfigEditorColour
    public String highlightColor = "0:249:0:255:88";

    @Expose
    @ConfigOption(name = "Steak Alert", desc = "Show a title when you can steak the boss.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean steakAlert = true;

    @Expose
    @ConfigOption(name = "Twinclaws Title", desc = "Send a title when Twinclaws is about to happen.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean twinClawsTitle = true;
}
