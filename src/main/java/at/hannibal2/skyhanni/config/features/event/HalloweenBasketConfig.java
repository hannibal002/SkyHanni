package at.hannibal2.skyhanni.config.features.event;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class HalloweenBasketConfig {

    @Expose
    @ConfigOption(name = "Basket Waypoints", desc = "Show all Halloween Basket waypoints.\nShoutout to §bTobbbb §7for the coordinates.\n(AS OF 2023)")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean allWaypoints = false;

    @Expose
    @ConfigOption(name = "Entrance Waypoints", desc = "Show helper waypoints to Baskets #23, #24, and #25. Coordinates by §bErymanthus§7.")
    @ConfigEditorBoolean
    public boolean allEntranceWaypoints = false;

    @Expose
    @ConfigOption(name = "Only Closest", desc = "Only show the closest waypoint")
    @ConfigEditorBoolean
    public boolean onlyClosest = true;
}
