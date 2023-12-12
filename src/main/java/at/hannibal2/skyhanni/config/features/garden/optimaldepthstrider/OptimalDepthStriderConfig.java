package at.hannibal2.skyhanni.config.features.garden.optimaldepthstrider;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class OptimalDepthStriderConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Show the optimal depth strider for your current tool in the hand.\n" +
        "(Thanks MelonKingDE for the default values) (ask about this).")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Warning Title", desc = "Warn via title when you don't have the optimal depth strider.")
    @ConfigEditorBoolean
    public boolean warning = false;

    @Expose
    @ConfigOption(name = "Custom Depth Strider", desc = "Change the exact depth strider for every single crop.")
    @Accordion
    public CustomDepthStriderConfig customDepthStrider = new CustomDepthStriderConfig();

    public Position pos = new Position(5, -184, false, true);

}
