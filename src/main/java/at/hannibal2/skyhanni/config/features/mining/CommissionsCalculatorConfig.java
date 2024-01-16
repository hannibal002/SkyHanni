package at.hannibal2.skyhanni.config.features.mining;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class CommissionsCalculatorConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Calculates your commissions to the next HOTM Tier.\n§eDisclaimer: Does not account for HOTM XP gained from Daily Bonuses.")
    @ConfigEditorBoolean
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Completed Commissions Only", desc = "Only counts completed commissions as part of the Commission calculations.")
    @ConfigEditorBoolean
    public boolean completedOnly = false;

    @Expose
    @ConfigOption(name = "Progress for All Milestones", desc = "Calculates remaining commissions for all incomplete Commission Milestones.")
    @ConfigEditorBoolean
    public boolean allMilestones = false;

    @Expose
    public Position position = new Position(100, 90, false, true);
}
