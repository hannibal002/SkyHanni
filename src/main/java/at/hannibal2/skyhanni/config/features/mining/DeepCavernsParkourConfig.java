package at.hannibal2.skyhanni.config.features.mining;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorColour;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

public class DeepCavernsParkourConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Shows a parkour to the bottom of Deep Caverns and to Rhys.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Look Ahead", desc = "Change how many waypoints should be shown in front of you.")
    @ConfigEditorSlider(minStep = 1, maxValue = 30, minValue = 1)
    public Property<Integer> lookAhead = Property.of(3);

    @Expose
    @ConfigOption(name = "Rainbow Color", desc = "Show the rainbow color effect instead of a boring monochrome.")
    @ConfigEditorBoolean
    public Property<Boolean> rainbowColor = Property.of(true);

    @Expose
    @ConfigOption(name = "Monochrome Color", desc = "Set a boring monochrome color for the parkour platforms.")
    @ConfigEditorColour
    public Property<String> monochromeColor = Property.of("0:60:0:0:255");

    @Expose
    @ConfigOption(name = "Hilight all Lines", desc = "Show all lines in gray. Useful for debugging.")
    @ConfigEditorBoolean
    public boolean highlightAllLines = false;
}
