package at.hannibal2.skyhanni.config.features.event.winter;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class UniqueGiftConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Show in a display how many unique players you have given gifts to in the winter 2023 event." +
        "Open §e/opengenerowmenu §7to sync up!")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    public Position position = new Position(100, 100, false, true);
}
