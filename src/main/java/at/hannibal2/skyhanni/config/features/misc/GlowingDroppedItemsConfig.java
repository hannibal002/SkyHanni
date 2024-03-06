package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class GlowingDroppedItemsConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Draws a glowing outline around all dropped items on the ground.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Highlight Showcase Items", desc = "Draws a glowing outline around showcase items.")
    @ConfigEditorBoolean
    public boolean highlightShowcase = false;

    @Expose
    @ConfigOption(name = "Highlight Fishing Bait", desc = "Draws a glowing outline around fishing bait.")
    @ConfigEditorBoolean
    public boolean highlightFishingBait = false;

}
