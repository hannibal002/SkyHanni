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
    @ConfigOption(name = "Switch Notification", desc = "Sends a notification when approaching the end of a lane and you should switch lanes.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean switchNotification = false;

    @Expose
    @ConfigOption(name = "Distance until Switch", desc = "Displays the remaining distance until the next switch.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean distanceUntilSwitch = false;

    @Expose
    public Position distanceUntilSwitchPosition = new Position(0, 200, false, true);

    @Expose
    @ConfigOption(name = "Corner Waypoints", desc = "Show the corner for the current lane in the world.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean cornerWaypoints = false;

    @Expose
    @ConfigOption(name = "Sound Settings", desc = "")
    @Accordion
    public LaneSwitchSoundSettings switchSounds = new LaneSwitchSoundSettings();

    @Expose
    @ConfigOption(name = "Notification Settings", desc = "")
    @Accordion
    public LaneSwitchNotificationConfig switchSettings = new LaneSwitchNotificationConfig();

}
