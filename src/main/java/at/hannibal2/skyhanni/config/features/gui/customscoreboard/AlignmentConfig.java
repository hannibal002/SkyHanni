package at.hannibal2.skyhanni.config.features.gui.customscoreboard;

import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment;
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class AlignmentConfig {

    @Expose
    @ConfigOption(name = "Horizontal Alignment", desc = "Alignment for the scoreboard on the horizontal axis.")
    @ConfigEditorDropdown
    public HorizontalAlignment horizontalAlignment = HorizontalAlignment.RIGHT;

    @Expose
    @ConfigOption(name = "Vertical Alignment", desc = "Alignment for the scoreboard on the vertical axis.")
    @ConfigEditorDropdown
    public VerticalAlignment verticalAlignment = VerticalAlignment.CENTER;
}
