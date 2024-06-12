package at.hannibal2.skyhanni.config.features.dungeon;

import com.google.gson.annotations.Expose;

import at.hannibal2.skyhanni.config.FeatureToggle;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class LividFinderConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Help find the correct livid in F5 and in M5.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Hide Wrong Livids", desc = "Hide wrong livids entirely.")
    @ConfigEditorBoolean
    public boolean hideWrong = false;
}
