package at.hannibal2.skyhanni.config.features.garden.laneswitch;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorInfoText;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class FarmingLaneConfig {

    @Expose
    @ConfigOption(name = "Create Lanes", desc = "In order for those features to work, you first need to create a lane with §e/shlanedetection§7!")
    @ConfigEditorInfoText(infoTitle = "Tutorial")
    public boolean tutorial = false;

    @Expose
    @ConfigOption(name = "Lane Switch Notification", desc = "")
    @Accordion
    public LaneSwitchNotificationConfig laneSwitchNotification = new LaneSwitchNotificationConfig();

    @Expose
    @ConfigOption(name = "Distance Display", desc = "Shows the remaining distance and time until you reach the end of the current lane.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean distanceDisplay = false;

    @Expose
    public Position distanceDisplayPosition = new Position(0, 200, false, true);

    @Expose
    @ConfigOption(name = "Corner Waypoints", desc = "Show the corner for the current lane in the world.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean cornerWaypoints = false;

}
