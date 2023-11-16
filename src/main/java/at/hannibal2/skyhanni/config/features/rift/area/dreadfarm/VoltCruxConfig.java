package at.hannibal2.skyhanni.config.features.rift.area.dreadfarm;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorColour;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

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
    @ConfigOption(name = "Volt Range Highlighter Color", desc = "In which color should the Volt range be highlighted?")
    @ConfigEditorColour
    public String voltColour = "0:60:0:0:255";

    @Expose
    @ConfigOption(name = "Volt Mood Color", desc = "Change the color of the Volt enemy depending on their mood.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean voltMoodMeter = false;
}
