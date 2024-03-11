package at.hannibal2.skyhanni.config.features.garden.laneswitch;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class LaneSwitchNotificationConfig {

    @Expose
    @ConfigOption(name = "Sound Settings", desc = "")
    @Accordion
    public LaneSwitchSoundSettings sound = new LaneSwitchSoundSettings();

    @Expose
    @ConfigOption(name = "Notification Settings", desc = "")
    @Accordion
    public LaneSwitchNotificationSettings settings = new LaneSwitchNotificationSettings();
}
