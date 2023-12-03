package at.hannibal2.skyhanni.config.features.gui.customscoreboard;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

public class PartyConfig {
    @Expose
    @ConfigOption(name = "Max Party List", desc = "Max number of party members to show in the party list. (You are not included)")
    @ConfigEditorSlider(
        minValue = 0,
        maxValue = 25, // why do I even set it so high
        minStep = 1
    )
    public Property<Integer> maxPartyList = Property.of(4);

    @Expose
    @ConfigOption(name = "Show Party everywhere", desc = "Show the party list everywhere.\nIf disabled, it will only show in Dungeon hub, Crimson Isle & Kuudra")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean showPartyEverywhere = false;
}
