package at.hannibal2.skyhanni.config.features.dungeon;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class LividFinderConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Helps find the correct livid in F5 and in M5.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Hide Wrong Livids", desc = "Hide wrong livids entirely.")
    @ConfigEditorBoolean
    public boolean hideWrong = false;
}
