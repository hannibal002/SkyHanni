package at.hannibal2.skyhanni.config.features.garden;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class GardenLevelConfig {
    @Expose
    @ConfigOption(name = "Display", desc = "Show the current Garden level and progress to the next level.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean display = true;

    @Expose
    public Position pos = new Position(390, 40, false, true);
}
