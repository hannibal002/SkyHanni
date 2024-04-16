package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class NoBitsWarningConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Alerts you when you have no bits available.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Notification Sound", desc = "Plays a notification sound when you get a warning.")
    @ConfigEditorBoolean
    public boolean notificationSound = true;
}
