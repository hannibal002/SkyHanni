package at.hannibal2.skyhanni.config.features.slayer.vampire;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class CoopBossHighlightConfig {
    @Expose
    @ConfigOption(name = "Highlight Co-op Boss", desc = "Highlight boss of your co-op member.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean highlight = true;

    @Expose
    @ConfigOption(name = "Highlight Color", desc = "What color to highlight the boss in.")
    @ConfigEditorColour
    public String highlightColor = "0:249:0:255:88";

    @Expose
    @ConfigOption(name = "Co-op Members", desc = "Add your co-op member here.\n§eFormat: §7Name1,Name2,Name3")
    @ConfigEditorText
    public String coopMembers = "";

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

    @Expose
    @ConfigOption(name = "Twinclaws Sound", desc = "Play a sound when Twinclaws is about to happen.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean twinClawsSound = true;
}
