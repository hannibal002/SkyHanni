package at.hannibal2.skyhanni.config.features.fishing;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class FishedItemNameConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Show the fished item name above the item when fishing.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Show Bait", desc = "Also show the name of the consumed bait.")
    @ConfigEditorBoolean
    public boolean showBaits = false;

}
