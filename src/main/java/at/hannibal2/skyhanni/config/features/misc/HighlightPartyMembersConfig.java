package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorColour;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class HighlightPartyMembersConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Marking party members with a bright outline to better find them in the world.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(
        name = "Outline Color",
        desc = "The color to outline party members in."
    )
    @ConfigEditorColour
    public String outlineColor = "0:245:85:255:85";

}
