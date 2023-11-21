package at.hannibal2.skyhanni.config.features.stranded;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class StrandedConfig {
    @Expose
    @ConfigOption(name = "Highlight Placeable NPCs", desc = "Highlight NPCs that can be placed, but aren't, in the NPC menu.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean highlightPlaceableNpcs = false;
}
