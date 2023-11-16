package at.hannibal2.skyhanni.config.features.slayer.vampire;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorColour;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class OwnBossConfig {

    @Expose
    @ConfigOption(name = "Highlight Your Boss", desc = "Highlight your own Vampire Slayer boss.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean highlight = true;

    @Expose
    @ConfigOption(name = "Highlight Color", desc = "What color to highlight the boss in.")
    @ConfigEditorColour
    public String highlightColor = "0:249:0:255:88";

    @Expose
    @ConfigOption(name = "Steak Alert", desc = "Show a title when you can steak your boss.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean steakAlert = true;

    @Expose
    @ConfigOption(name = "Twinclaws Title", desc = "Send a title when Twinclaws is about to happen.\nWork on others highlighted people boss.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean twinClawsTitle = true;

    @Expose
    @ConfigOption(name = "Twinclaws Sound", desc = "Play a sound when Twinclaws is about to happen.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean twinClawsSound = true;
}
