package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class PlayerStatsDisplayConfig {
    @Expose
    @ConfigOption(name = "Mana Display", desc = "Show the estimated mana and max mana as a display.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean mana = false;

    @Expose
    public Position manaPosition = new Position(-330, -15, false, true);
}
