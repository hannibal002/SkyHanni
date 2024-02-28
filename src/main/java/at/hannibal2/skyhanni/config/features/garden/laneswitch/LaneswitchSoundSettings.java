package at.hannibal2.skyhanni.config.features.garden.laneswitch;

import at.hannibal2.skyhanni.data.GuiEditManager;
import at.hannibal2.skyhanni.features.garden.farming.LaneswitchNotification;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorButton;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigEditorText;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class LaneswitchSoundSettings {
    @Expose
    @ConfigOption(name = "Notification Sound", desc = "The Sound played for the Notification.")
    @ConfigEditorText
    public String notificationSound = "random.pling";

    @Expose
    @ConfigOption(name = "Notification Pitch", desc = "The Pitch of the notification Sound.")
    @ConfigEditorSlider(
        minValue = 0.5f,
        maxValue = 2.0f,
        minStep = 0.1f
    )
    public float notificationPitch = 1.0f;

    @Expose
    @ConfigOption(name = "Notification Volume", desc = "The Volume of the notification Sound.")
    @ConfigEditorSlider(
        minValue = 1f,
        maxValue = 100f,
        minStep = 1f
    )
    public float notificationVolume = 50f;

    @Expose
    @ConfigOption(name = "Test Sound", desc = "Test the Notification Sound.")
    @ConfigEditorButton(buttonText = "Test")
    public Runnable soundTester = LaneswitchNotification::playUserSound;
}
