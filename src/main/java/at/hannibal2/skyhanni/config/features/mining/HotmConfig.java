package at.hannibal2.skyhanni.config.features.mining;

import com.google.gson.annotations.Expose;

import at.hannibal2.skyhanni.config.FeatureToggle;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class HotmConfig {

    @Expose
    @ConfigOption(name = "Enabled Highlight", desc = "Highlight enabled perks in the hotm tree green and disabled red. Locked perks are highlighted gray.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean highlightEnabledPerks = true;

    @Expose
    @ConfigOption(name = "Level Stack", desc = "Show the level of a perk as item stacks.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean levelStackSize = true;

    @Expose
    @ConfigOption(name = "Token Stack", desc = "Show unused tokens on the heart.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean tokenStackSize = true;
}
