package at.hannibal2.skyhanni.config.features.event.waypoints;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class ChristmasPresentConfig {

    @Expose
    @ConfigOption(name = "Present Waypoints", desc = "Show all Present waypoints")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean allWaypoints = false;

    @Expose
    @ConfigOption(name = "Entrance Waypoints", desc = "Show helper waypoints to .")
    @ConfigEditorBoolean
    public boolean allEntranceWaypoints = false;

    @Expose
    @ConfigOption(name = "Only Closest", desc = "Only show the closest waypoint")
    @ConfigEditorBoolean
    public boolean onlyClosest = false;
}
