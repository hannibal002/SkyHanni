package at.hannibal2.skyhanni.config.features.inventory;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class JacobFarmingContestConfig {
    @Expose
    @ConfigOption(name = "Unclaimed Rewards", desc = "Highlight contests with unclaimed rewards in the Jacob inventory.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean highlightRewards = true;

    @Expose
    @ConfigOption(name = "Contest Time", desc = "Adds the real time format to the Contest description.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean realTime = true;

    @Expose
    @ConfigOption(name = "Medal Icon", desc = "Adds a symbol that shows what medal you received in this Contest. " +
        "§eIf you use a texture pack this may cause conflicting icons.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean medalIcon = true;

    @Expose
    @ConfigOption(name = "Finnegan Icon", desc = "Uses a different indicator for when the Contest happened during Mayor Finnegan.")
    @ConfigEditorBoolean
    public boolean finneganIcon = true;
}
