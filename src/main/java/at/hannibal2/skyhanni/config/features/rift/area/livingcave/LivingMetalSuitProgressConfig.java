package at.hannibal2.skyhanni.config.features.rift.area.livingcave;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class LivingMetalSuitProgressConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Display Living Metal Suit progress.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Compact", desc = "Show a compacted version of the overlay when the set is maxed.")
    @ConfigEditorBoolean
    public boolean compactWhenMaxed = false;

    @Expose
    public Position position = new Position(100, 100);
}
