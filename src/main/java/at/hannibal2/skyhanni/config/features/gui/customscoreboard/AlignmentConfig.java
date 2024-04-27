package at.hannibal2.skyhanni.config.features.gui.customscoreboard;

import at.hannibal2.skyhanni.utils.RenderUtils;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class AlignmentConfig {

    @Expose
    @ConfigOption(name = "Align Horizontally", desc = "Align the scoreboard on the horizontal axis.")
    @ConfigEditorDropdown
    public RenderUtils.HorizontalAlignment horizontalAlignment = RenderUtils.HorizontalAlignment.RIGHT;

    @Expose
    @ConfigOption(name = "Align Vertically", desc = "Align the scoreboard on the vertical axis.")
    @ConfigEditorDropdown
    public RenderUtils.VerticalAlignment verticalAlignment = RenderUtils.VerticalAlignment.CENTER;
}
