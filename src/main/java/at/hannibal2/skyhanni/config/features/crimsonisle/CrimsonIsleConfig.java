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
    @ConfigOption(name = "Pablo NPC Helper", desc = "Shows a clickable message that grabs the flower needed from your sacks.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean pabloHelper = false;

    @Expose
    @ConfigOption(name = "Volcano Explosivity", desc = "Shows a HUD of the current volcano explosivity level.")
    @ConfigEditorBoolean
    public boolean volcanoExplosivity = false;

    @Expose
    public Position positionVolcano = new Position(20, 20, false, true);

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
