package at.hannibal2.skyhanni.config.features.garden.laneswitch;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigEditorText;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class LaneSwitchNotificationConfig {

    @Expose
    @ConfigOption(name = "Notification Text", desc = "The text with color to be displayed as the notification.")
    @ConfigEditorText
    public String text = "&eLane Switch incoming.";

    @Expose
    @ConfigOption(name = "Seconds Before", desc = "How many seconds before reaching the end of the lane should the warning happen?")
    @ConfigEditorSlider(
        minValue = 1,
        maxValue = 10,
        minStep = 1
    )
    public int secondsBefore = 5;
}
