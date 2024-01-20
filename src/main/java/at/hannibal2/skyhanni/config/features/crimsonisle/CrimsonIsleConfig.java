package at.hannibal2.skyhanni.config.features.crimsonisle;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.config.features.crimsonisle.ashfang.AshfangConfig;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.Category;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class CrimsonIsleConfig {

    @Category(name = "Ashfang", desc = "Ashfang settings")
    @Expose
    public AshfangConfig ashfang = new AshfangConfig();

    @ConfigOption(name = "Reputation Helper", desc = "")
    @Accordion
    @Expose
    public ReputationHelperConfig reputationHelper = new ReputationHelperConfig();

    @Expose
    @ConfigOption(name = "Quest Item Helper", desc = "When you open the fetch item quest in the town board, " +
        "it shows a clickable chat message that will grab the items needed from the sacks.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean questItemHelper = false;

    @Expose
    @ConfigOption(name = "Pablo NPC Helper", desc = "Similar to Quest Item Helper, shows a clickable message that grabs the flower needed from sacks.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean pabloHelper = false;

    @Expose
    @ConfigOption(name = "Dojo Rank Display", desc = "Display your rank, score, actual belt\n" +
        "and points needed for the next belt in the Challenges inventory\n" +
        "on the Crimson Isles.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean showDojoRankDisplay = false;

    @Expose
    public Position dojoRankDisplayPosition = new Position(-378, 206, false, true);
}
