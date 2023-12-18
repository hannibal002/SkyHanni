package at.hannibal2.skyhanni.config.features.garden;

import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class MouseSensConfig {
    @Expose
    @ConfigOption(
        name = "Lower Mouse Sensitivity",
        desc = "Lowers mouse sensitivity while a farming tool is held.")
    @ConfigEditorBoolean
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Sensitivity Divisor", desc = "Changes how much the features lowers the sensitivity by.")
    @ConfigEditorSlider(minValue = 0, maxValue = 100, minStep = 1)
    public int divisorSens = 25;

    @Expose
    @ConfigOption(
        name = "Show GUI",
        desc = "Shows the GUI element while the feature is enabled.")
    @ConfigEditorBoolean
    public boolean showLower = true;

    @Expose
    public Position loweredMouseDisplay = new Position(400, 400, 0.8f);
}
