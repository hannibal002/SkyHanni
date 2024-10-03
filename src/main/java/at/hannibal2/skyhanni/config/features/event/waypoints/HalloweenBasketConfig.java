package at.hannibal2.skyhanni.config.features.event.waypoints;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class HalloweenBasketConfig {

    @Expose
    @ConfigOption(name = "Basket Waypoints", desc = "Show all Halloween Basket waypoints.\n" +
        "§eCoordinates may not always be up to date! §7(Last updated: 2024)")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean allWaypoints = false;

    @Expose
    @ConfigOption(name = "Only Closest", desc = "Only show the closest waypoint.")
    @ConfigEditorBoolean
    public boolean onlyClosest = true;

    @Expose
    @ConfigOption(name = "Pathfind", desc = "Show a path to the closest basket.")
    @ConfigEditorBoolean
    public boolean pathfind = true;
}
