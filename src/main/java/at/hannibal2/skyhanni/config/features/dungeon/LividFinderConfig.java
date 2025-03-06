package at.hannibal2.skyhanni.config.features.dungeon;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.features.dungeon.DungeonLividFinder;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.observer.Property;

public class LividFinderConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Help find the correct livid in F5 and in M5.")
    @ConfigEditorBoolean
    @FeatureToggle
    public Property<Boolean> enabled = Property.of(false);

    @Expose
    @ConfigOption(name = "Hide Wrong Livids", desc = "Hide wrong livids entirely.")
    @ConfigEditorBoolean
    public boolean hideWrong = false;

    @Expose
    @ConfigOption(name = "Color Override", desc = "Forces the livid highlight to be a specific color.")
    @ConfigEditorDropdown
    public DungeonLividFinder.LividColorHighlight colorOverride = DungeonLividFinder.LividColorHighlight.DEFAULT;
}
