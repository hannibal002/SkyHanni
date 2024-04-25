package at.hannibal2.skyhanni.config.features.gui.customscoreboard;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class AlignmentConfig {
    // TODO: Switch to Dropdowns with multiple different alignment ways in the future
    // Horizontal: Left, Center, Right
    // Vertical: Top, Center, Bottom
    @Expose
    @ConfigOption(name = "Align to the right", desc = "Align the scoreboard to the right side of the screen.")
    @ConfigEditorBoolean
    public boolean alignRight = true;

    @Expose
    @ConfigOption(name = "Align to the center vertically", desc = "Align the scoreboard to the center of the screen vertically.")
    @ConfigEditorBoolean
    public boolean alignCenterVertically = true;
}
