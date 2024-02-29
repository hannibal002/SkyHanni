package at.hannibal2.skyhanni.config.features.garden.laneswitch;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.utils.LorenzColor;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorColour;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class LaneswitchNotificationConfig {

    @Expose
    @ConfigOption(name = "Sound Settings", desc = "")
    @Accordion
    public LaneswitchSoundSettings sound = new LaneswitchSoundSettings();

    @Expose
    @ConfigOption(name = "Notification Settings", desc = "")
    @Accordion
    public LaneswitchNotificationSettings settings = new LaneswitchNotificationSettings();
}
