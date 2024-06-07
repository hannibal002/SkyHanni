package at.hannibal2.skyhanni.config.features.gui;

import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class HealthDisplay {
    @Expose
    @ConfigOption(
        name = "enable bar",
        desc = "show bar"
    )
    @ConfigEditorBoolean
    public Boolean enabledBar = false;

    @Expose
    @ConfigLink(owner = HealthDisplay.class, field = "enabledBar")
    public Position positionBar = new Position(40, 40, 1.0f);

    @Expose
    @ConfigOption(
        name = "enable text",
        desc = "show text"
    )
    @ConfigEditorBoolean
    public Boolean enabledText = false;

    @Expose
    @ConfigLink(owner = HealthDisplay.class, field = "enabledText")
    public Position positionText = new Position(40, 40, 1.0f);

    @Expose
    @ConfigOption(
        name = "health predictor",
        desc = "faster; less precise"
    )
    @ConfigEditorBoolean
    public Boolean predictHealth = true;

    @Expose
    @ConfigOption(
        name = "health updates",
        desc = "it shows health updates"
    )
    @ConfigEditorBoolean
    public Boolean healthUpdates = false;
}
