package at.hannibal2.skyhanni.config.features.gui;

import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class HealthDisplay {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Show a Health Bar."
    )
    @ConfigEditorBoolean
    public Boolean enabledBar = false;

    @Expose
    @ConfigLink(owner = HealthDisplay.class, field = "enabled")
    public Position positionBar = new Position(40, 40, 1.0f);

    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Show a Health Bar."
    )
    @ConfigEditorBoolean
    public Boolean enabledText = false;

    @Expose
    @ConfigLink(owner = HealthDisplay.class, field = "enabled")
    public Position positionText = new Position(40, 40, 1.0f);
}
