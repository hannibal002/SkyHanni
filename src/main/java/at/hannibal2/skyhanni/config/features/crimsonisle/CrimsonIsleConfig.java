package at.hannibal2.skyhanni.config.features.crimsonisle;

import com.google.gson.annotations.Expose;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.config.features.crimsonisle.ashfang.AshfangConfig;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.Category;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class CrimsonIsleConfig {

    @Category(name = "Ashfang", desc = "Ashfang settings")
    @Expose
    public AshfangConfig ashfang = new AshfangConfig();

    @ConfigOption(name = "Reputation Helper", desc = "")
    @Accordion
    @Expose
    public ReputationHelperConfig reputationHelper = new ReputationHelperConfig();

    @Expose
    @ConfigOption(name = "Matriach Helper", desc = "Helper for Heavy Pearls")
    @Accordion
    public MatriarchHelperConfig matriarchHelper = new MatriarchHelperConfig();

    @Expose
    @ConfigOption(name = "Pablo NPC Helper", desc = "Show a clickable message that grabs the flower needed from your sacks.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean pabloHelper = false;

    @Expose
    @ConfigOption(name = "Volcano Explosivity", desc = "Show a HUD of the current volcano explosivity level.")
    @ConfigEditorBoolean
    public boolean volcanoExplosivity = false;

    @Expose
    @ConfigLink(owner = CrimsonIsleConfig.class, field = "volcanoExplosivity")
    public Position positionVolcano = new Position(20, 20, false, true);

    @Expose
    @ConfigOption(name = "Dojo Rank Display", desc = "Display your rank, score, actual belt, and points needed for the next belt in the Challenges inventory on the Crimson Isles.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean showDojoRankDisplay = false;

    @Expose
    @ConfigLink(owner = CrimsonIsleConfig.class, field = "showDojoRankDisplay")
    public Position dojoRankDisplayPosition = new Position(-378, 206, false, true);

}
