package at.hannibal2.skyhanni.config.features.garden;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class YawPitchDisplayConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Displays yaw and pitch while holding a farming tool. Automatically fades out if there is no movement.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Yaw Precision", desc = "Yaw precision up to specified decimal.")
    @ConfigEditorSlider(
        minValue = 1,
        maxValue = 10,
        minStep = 1
    )
    public int yawPrecision = 4;

    @Expose
    @ConfigOption(name = "Pitch Precision", desc = "Pitch precision up to specified decimal.")
    @ConfigEditorSlider(
        minValue = 1,
        maxValue = 10,
        minStep = 1
    )
    public int pitchPrecision = 4;

    @Expose
    @ConfigOption(name = "Display Timeout", desc = "Duration in seconds for which the overlay is being displayed after moving.")
    @ConfigEditorSlider(
        minValue = 1,
        maxValue = 20,
        minStep = 1
    )
    public int timeout = 5;

    @Expose
    @ConfigOption(name = "Show Without Tool", desc = "Does not require you to hold a tool for the overlay to show.")
    @ConfigEditorBoolean
    public boolean showWithoutTool = false;

    @Expose
    @ConfigOption(name = "Show Outside Garden", desc = "The overlay will work outside of the Garden.")
    @ConfigEditorBoolean
    public boolean showEverywhere = false;

    @Expose
    @ConfigOption(name = "Ignore Timeout", desc = "Ignore the timeout after not moving mouse.")
    @ConfigEditorBoolean
    public boolean showAlways = false;

    @Expose
    public Position pos = new Position(445, 225, false, true);
    @Expose
    public Position posOutside = new Position(445, 225, false, true);
}
