package at.hannibal2.skyhanni.config.features.mining;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.Category;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class MiningConfig {

    @Expose
    @Category(name = "Mining Event Tracker", desc = "Settings for the Mining Event Tracker")
    public MiningEventConfig miningEvent = new MiningEventConfig();

    @Expose
    @ConfigOption(name = "Powder Tracker", desc = "")
    @Accordion
    public PowderTrackerConfig powderTracker = new PowderTrackerConfig();

    @Expose
    @ConfigOption(name = "King Talisman", desc = "")
    @Accordion
    public KingTalismanConfig kingTalisman = new KingTalismanConfig();

    @Expose
    @ConfigOption(name = "Deep Caverns Parkour", desc = "")
    @Accordion
    public DeepCavernsParkourConfig deepCavernsParkour = new DeepCavernsParkourConfig();

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

    @Expose
    @ConfigOption(name = "Private Island Ability Block", desc = "Blocks the mining ability when on private island.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean privateIslandNoPickaxeAbility = false;
}
