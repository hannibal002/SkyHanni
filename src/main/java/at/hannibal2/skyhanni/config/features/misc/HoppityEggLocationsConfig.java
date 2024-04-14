package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class HoppityEggLocationsConfig {

    @Expose
    @ConfigOption(name = "Hoppity Waypoints", desc = "Toggle guess waypoints for Hoppity's Hunt.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean waypointsEnabled = true;

    // todo remove probably or make false or something
    @Expose
    @ConfigOption(name = "Show All Waypoints", desc = "Show all possible egg waypoints for the current lobby.")
    @ConfigEditorBoolean
    public boolean showAllWaypoints = true;
}
