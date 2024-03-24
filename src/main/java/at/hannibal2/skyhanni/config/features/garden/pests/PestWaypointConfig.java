package at.hannibal2.skyhanni.config.features.garden.pests;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class PestWaypointConfig {

    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Show a waypoint to the pest when using a vacuum."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    @ConfigOption(
        name = "Hide Particles",
        desc = "Hide the particles of the ability."
    )
    @ConfigEditorBoolean
    public boolean hideParticles = true;

    @Expose
    @ConfigOption(
        name = "Show For Seconds",
        desc = "Shows waypoint for that amount of seconds."
    )
    @ConfigEditorSlider(minStep = 1, minValue = 5, maxValue = 20)
    public double showWaypointForSeconds = 10;
}
