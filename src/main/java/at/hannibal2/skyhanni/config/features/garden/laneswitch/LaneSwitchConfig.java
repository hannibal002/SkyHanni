package at.hannibal2.skyhanni.config.features.garden.laneswitch;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class LaneSwitchConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Sends a notification when approaching the end of a lane.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Distance until Switch", desc = "Displays the remaining distance until the next switch.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean distanceUntilSwitch = false;

    @Expose
    public Position distanceUntilSwitchPos = new Position(0, 200, false, true);

    @Expose
    @ConfigOption(name = "Notifications", desc = "")
    @Accordion
    public LaneSwitchNotificationConfig notification = new LaneSwitchNotificationConfig();

}
