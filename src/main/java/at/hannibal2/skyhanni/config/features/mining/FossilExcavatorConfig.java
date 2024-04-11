package at.hannibal2.skyhanni.config.features.mining;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class FossilExcavatorConfig {

    @Expose
    @ConfigOption(name = "Fossil Excavator Helper", desc = "Helps you find fossils in the fossil excavator.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    @ConfigLink(owner = FossilExcavatorConfig.class, field = "enabled")
    public Position position = new Position(183, 212, false, true);

}
