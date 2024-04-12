package at.hannibal2.skyhanni.config.features.mining;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class ExcavatorProfitTrackerConfig {

    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Count all drops you gain while excavating fossils in the Glacial Cave."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    @ConfigOption(
        name = "Show nearby",
        desc = "Show the profit tracker while close to the excavation point."
    )
    @ConfigEditorBoolean
    public boolean showNearvy = true;

    @Expose
    @ConfigLink(owner = ExcavatorProfitTrackerConfig.class, field = "enabled")
    public Position position = new Position(20, 20, false, true);
}
