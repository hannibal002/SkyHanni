package at.hannibal2.skyhanni.config.features.rift.area.dreadfarm;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class VoltCruxConfig {

    @Expose
    @ConfigOption(name = "Volt Warning", desc = "Shows a warning while a Volt is discharging lightning.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean voltWarning = true;

    @Expose
    @ConfigOption(name = "Volt Range Highlighter", desc = "Shows the area in which a Volt might strike lightning.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean voltRange = true;

    @Expose
    @ConfigOption(name = "Volt Range Highlighter Colour", desc = "Which colour the Volt range will be highlighted in.")
    @ConfigEditorColour
    public String voltColour = "0:60:0:0:255";

    @Expose
    @ConfigOption(name = "Volt Mood Colour", desc = "Change the colour of the Volt enemy depending on their mood.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean voltMoodMeter = false;
}
