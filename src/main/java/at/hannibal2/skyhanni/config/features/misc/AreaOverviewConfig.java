package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class AreaOverviewConfig {
    @Expose
    @ConfigOption(name = "Display", desc = "Show a list of all areas on the island.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean display = false;

    @Expose
    @ConfigOption(name = "In World", desc = "Show the area names in world")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean inWorld = false;

    @Expose
    @ConfigLink(owner = AreaOverviewConfig.class, field = "display")
    public Position position = new Position(20, 20);
}
