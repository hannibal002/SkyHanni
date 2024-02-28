package at.hannibal2.skyhanni.config.features.garden.laneswitch;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class LaneSwitchConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Sends a notification when approaching the end of a lane. Settings below.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Farm Layout", desc = "")
    @Accordion
    public LaneswitchFarmConfig farm = new LaneswitchFarmConfig();

    @Expose
    @ConfigOption(name = "Notifications", desc = "")
    @Accordion
    public LaneswitchNotificationConfig notification = new LaneswitchNotificationConfig();

}
