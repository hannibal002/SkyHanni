package at.hannibal2.skyhanni.config.features.mining;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class KingTalismanConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Show kings you have not talked to yet, and when the next missing king will appear.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Outside Mines", desc = "Show the display even while outside the Dwarven Mines.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean outsideMines = false;

    @Expose
    public Position position = new Position(-400, 220, false, true);
}
