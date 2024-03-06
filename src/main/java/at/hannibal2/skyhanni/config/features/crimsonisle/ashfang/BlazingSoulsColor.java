package at.hannibal2.skyhanni.config.features.crimsonisle.ashfang;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorColour;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class BlazingSoulsColor {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Shows the Blazing Souls more clearly.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Souls Color", desc = "Color of the Blazing Souls.")
    @ConfigEditorColour
    public String color = "0:245:85:255:85";
}
