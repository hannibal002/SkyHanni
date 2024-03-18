package at.hannibal2.skyhanni.config.features.garden.laneswitch;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigEditorText;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class LaneSwitchNotificationSettings {

    @Expose
    @ConfigOption(name = "Notification Text", desc = "The text with color to be displayed as the notification.")
    @ConfigEditorText
    public String text = "&eLane Switch incoming.";

    @Expose
    @ConfigOption(name = "Duration", desc = "The time the notification is displayed.")
    @ConfigEditorSlider(
        minValue = 1F,
        maxValue = 10F,
        minStep = 0.5F
    )
    public double duration = 2.5;

    @Expose
    @ConfigOption(name = "Seconds Before", desc = "How many seconds before reaching the end of the lane should the warning happen?")
    @ConfigEditorSlider(
        minValue = 1,
        maxValue = 10,
        minStep = 1
    )
    public int secondsBefore = 5;
}
