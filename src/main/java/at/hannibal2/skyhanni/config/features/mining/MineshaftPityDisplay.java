package at.hannibal2.skyhanni.config.features.mining;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class MineshaftPityDisplay {
    @Expose
    @ConfigOption(name = "Enable", desc = "")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enable = true;

    @Expose
    @ConfigLink(owner = MineshaftPityDisplay.class, field = "enable")
    public Position position = new Position(-330, -15, false, true);
}
