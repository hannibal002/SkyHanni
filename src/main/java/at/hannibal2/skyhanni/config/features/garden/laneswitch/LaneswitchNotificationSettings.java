package at.hannibal2.skyhanni.config.features.garden.laneswitch;

import at.hannibal2.skyhanni.utils.LorenzColor;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigEditorText;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class LaneswitchNotificationSettings {

    @Expose
    @ConfigOption(name = "Notification Text", desc = "The Text to be displayed as the Notification.")
    @ConfigEditorText
    public String notificationText = "Lane Switch incoming.";

    @ConfigOption(name = "Notification Text Color", desc = "Notification Text Color. §eIf Chroma is gray, enable Chroma in Chroma settings.")
    @Expose
    @ConfigEditorDropdown
    public LorenzColor notificationColor = LorenzColor.YELLOW;

    @Expose
    @ConfigOption(name = "Notification Duration", desc = "The time the Notification is displayed.")
    @ConfigEditorSlider(
        minValue = 1F,
        maxValue = 5F,
        minStep = 0.5F
    )
    public double notificationDuration = 2.5;

    @Expose
    @ConfigOption(name = "Notification Threshold", desc = "How early the notification will be displayed (Seconds before the Lane Switching Notification).")
    @ConfigEditorSlider(
        minValue = 1,
        maxValue = 15,
        minStep = 1
    )
    public int notificationThreshold = 10;

    @Expose
    @ConfigOption(name = "Notification Timeout", desc = "Minimum time to pass before the next Notification is displayed.")
    @ConfigEditorSlider(
        minValue = 15,
        maxValue = 90,
        minStep = 1
    )
    public int notificationTimeout = 30;
}
