package at.hannibal2.skyhanni.config.features.garden;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class CropStartLocationConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Show the start waypoint for the farm of your current tool in hand. Do §e/shcropstartlocation §7to change the waypoint again.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

}
