package at.hannibal2.skyhanni.config.features.gui.customscoreboard;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class AlignmentConfig {
    @Expose
    @ConfigOption(name = "Align to the right", desc = "Align the scoreboard to the right side of the screen.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean alignRight = false;

    @Expose
    @ConfigOption(name = "Align to the center vertically", desc = "Align the scoreboard to the center of the screen vertically.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean alignCenterVertically = false;
}
