package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class DataHistoryConfig {
    @Expose
    @ConfigOption(name = "Purse History", desc = "Save your coins in the purse over time. See the history with §e/shpursehistory§7.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean purse = false;
}
