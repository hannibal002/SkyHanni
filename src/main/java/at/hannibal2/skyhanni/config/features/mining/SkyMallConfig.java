package at.hannibal2.skyhanni.config.features.mining;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class SkyMallConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Shows an overlay with the current Sky Mall perk.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Outside Mines", desc = "Show the overlay even outside the mining islands.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean outsideMiningIslands = false;

    @Expose
    public Position skymallDisplayPosition = new Position(3, 140, 1.0f);
}
