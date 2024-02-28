package at.hannibal2.skyhanni.config.features.garden.laneswitch;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.features.garden.visitor.TimerConfig;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class LaneswitchConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Sends a Notification when approaching the end of a lane. Settings below.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Farm Config", desc = "")
    @Accordion
    public LaneswitchFarmConfig farm = new LaneswitchFarmConfig();

    @Expose
    @ConfigOption(name = "Notification Config", desc = "")
    @Accordion
    public LaneswitchNotificationConfig notification = new LaneswitchNotificationConfig();


}
