package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.OnlyLegacy;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class GlowingDroppedItemsConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Draw a glowing outline around all dropped items on the ground.")
    @ConfigEditorBoolean
    @FeatureToggle
    @OnlyLegacy
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Highlight Showcase Items", desc = "Draw a glowing outline around showcase items.")
    @ConfigEditorBoolean
    @OnlyLegacy
    public boolean highlightShowcase = false;

    @Expose
    @ConfigOption(name = "Highlight Fishing Bait", desc = "Draw a glowing outline around fishing bait.")
    @ConfigEditorBoolean
    @OnlyLegacy
    public boolean highlightFishingBait = false;

}
