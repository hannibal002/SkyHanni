package at.hannibal2.skyhanni.config.features.rift.area.colosseum;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class ColosseumConfig {

    @Expose
    @ConfigOption(name = "Highlight Blobbercysts", desc = "Highlight Blobbercysts in Bacte fight.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean highlightBlobbercysts = true;
}
