package at.hannibal2.skyhanni.config.features.garden;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class CropStartLocationConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Show the start waypoint for your farm with the currently holding tool.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

}
