package at.hannibal2.skyhanni.config.features.garden.laneswitch;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigEditorText;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class LaneswitchSoundSettings {
    @Expose
    @ConfigOption(name = "Notification Sound", desc = "The sound played for the notification. §7Check https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/mapping-and-modding-tutorials/2213619-1-8-all-playsound-sound-arguments for sound names.")
    @ConfigEditorText
    public String notificationSound = "random.orb";

    @Expose
    @ConfigOption(name = "Notification Pitch", desc = "The pitch of the notification sound.")
    @ConfigEditorSlider(minValue = 0.5f, maxValue = 2.0f, minStep = 0.1f)
    public float notificationPitch = 1.0f;
}
