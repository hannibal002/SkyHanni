package at.hannibal2.skyhanni.config.features.rift;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorColour;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class EnigmaSoulConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Click on Enigma Souls in Rift Guides to highlight their location.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Color", desc = "Color of the Enigma Souls.")
    @ConfigEditorColour
    public String color = "0:120:13:49:255";

}
