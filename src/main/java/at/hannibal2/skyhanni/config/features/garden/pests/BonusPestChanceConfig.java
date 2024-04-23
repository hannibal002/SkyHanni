package at.hannibal2.skyhanni.config.features.garden.pests;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class BonusPestChanceConfig {
    @Expose
    @ConfigOption(
        name = "Bonus Chance Display",
        desc = "Displays your bonus pest chance and if it is enabled or not."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean display = false;

    @Expose
    @ConfigLink(owner = BonusPestChanceConfig.class, field = "display")
    public Position pos = new Position(5, -115, false, true);
}
