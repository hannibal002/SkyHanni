package at.hannibal2.skyhanni.config.features.mining;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class MiningConfig {

    @Expose
    @ConfigOption(name = "Powder Tracker", desc = "")
    @Accordion
    public PowderTrackerConfig powderTracker = new PowderTrackerConfig();

    @Expose
    @ConfigOption(name = "King Talisman", desc = "")
    @Accordion
    public KingTalismanConfig kingTalisman = new KingTalismanConfig();

    @Expose
    @ConfigOption(name = "Sky Mall Display", desc = "")
    @Accordion
    public SkyMallConfig skymall = new SkyMallConfig();

    @Expose
    @ConfigOption(name = "Highlight Commission Mobs", desc = "Highlight Mobs that are part of active commissions.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean highlightCommissionMobs = false;

    @Expose
    @ConfigOption(name = "Names in Core", desc = "Show the names of the 4 areas while in the center of the Crystal Hollows.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean crystalHollowsNamesInCore = false;
}
