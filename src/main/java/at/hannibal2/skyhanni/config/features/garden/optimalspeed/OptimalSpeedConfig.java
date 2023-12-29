package at.hannibal2.skyhanni.config.features.garden.optimalspeed;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class OptimalSpeedConfig {
    @Expose
    @ConfigOption(name = "Show on HUD", desc = "Show the optimal speed for your current tool in the hand.\n" +
        "(Thanks MelonKingDE for the default values).")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean showOnHUD = false;

    @Expose
    @ConfigOption(name = "Warning Title", desc = "Warn via title when you don't have the optimal speed.")
    @ConfigEditorBoolean
    public boolean warning = false;

    @Expose
    @ConfigOption(name = "Rancher Boots", desc = "Allows you to set the optimal speed in the Rancher Boots overlay by clicking on the presets.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean signEnabled = true;

    @Expose
    public Position signPosition = new Position(20, -195, false, true);

    @Expose
    @ConfigOption(name = "Custom Speed", desc = "Change the exact speed for every single crop.")
    @Accordion
    public CustomSpeedConfig customSpeed = new CustomSpeedConfig();

    @Expose
    public Position pos = new Position(5, -200, false, true);
}
