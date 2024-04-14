package at.hannibal2.skyhanni.config.features.mining;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class ColdOverlayConfig {

    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Show the cold overlay in Glacite Tunnels."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(
        name = "Cold Threshold",
        desc = "The threshold at which the cold overlay will be shown."
    )
    @ConfigEditorSlider(
        minValue = 0f,
        maxValue = 100f,
        minStep = 1f
    )
    public float coldThreshold = 0f;

    @Expose
    @ConfigOption(
        name = "Max Opacity",
        desc = "The maximum opacity the overlay will reach."
    )
    @ConfigEditorSlider(
        minValue = 0f,
        maxValue = 100f,
        minStep = 0.1f
    )
    public float maxAlpha = 100f;
}
