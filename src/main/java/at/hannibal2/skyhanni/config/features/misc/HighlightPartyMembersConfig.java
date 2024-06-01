package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class HighlightPartyMembersConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Mark party members with a coloured outline.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(
        name = "Outline Colour",
        desc = "The colour to outline party members in."
    )
    @ConfigEditorColour
    public String outlineColor = "0:245:85:255:85";

}
