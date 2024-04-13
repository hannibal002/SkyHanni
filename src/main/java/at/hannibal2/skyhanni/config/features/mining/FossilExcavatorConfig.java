package at.hannibal2.skyhanni.config.features.mining;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class FossilExcavatorConfig {

    @Expose
    @ConfigOption(name = "Fossil Excavator Helper", desc = "Helps you find fossils in the fossil excavator. " +
        "§eWill always solve if you have at least 18 clicks. Solves everything except Spine, Ugly and Helix in 16 clicks.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Show Percentage", desc = "Shows percentage chance that next click will be a fossil. " +
        "§eThis assumes there is a fossil hidden in the dirt.")
    @ConfigEditorBoolean
    public boolean showPercentage = true;

    @Expose
    @ConfigLink(owner = FossilExcavatorConfig.class, field = "enabled")
    public Position position = new Position(183, 212, false, true);

}
