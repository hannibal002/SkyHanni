package at.hannibal2.skyhanni.config.features.crimsonisle.ashfang;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorColour;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class GravityOrbsConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Shows the Gravity Orbs more clearly.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Color", desc = "Color of the Gravity Orbs.")
    @ConfigEditorColour
    public String color = "0:120:255:85:85";
}
