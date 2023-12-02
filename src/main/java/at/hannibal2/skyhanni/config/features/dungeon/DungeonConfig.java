package at.hannibal2.skyhanni.config.features.dungeon;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class DungeonConfig {

    @Expose
    @ConfigOption(name = "Clicked Blocks", desc = "Highlight levers, chests, and Wither Essence when clicked in Dungeons.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean highlightClickedBlocks = false;

    @Expose
    @ConfigOption(name = "Milestones Display", desc = "Show the current milestone in Dungeons.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean showMilestonesDisplay = false;

    @Expose
    public Position showMileStonesDisplayPos = new Position(10, 10, false, true);

    @Expose
    @ConfigOption(name = "Death Counter Display", desc = "Display the total amount of deaths in the current Dungeon.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean deathCounterDisplay = false;

    @Expose
    public Position deathCounterPos = new Position(10, 10, false, true);

    @Expose
    @ConfigOption(name = "Clean End", desc = "")
    @Accordion
    public CleanEndConfig cleanEnd = new CleanEndConfig();

    @Expose
    @ConfigOption(name = "Boss Damage Splash", desc = "Hides damage splashes while inside the boss room (fixes a Skytils feature).")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean damageSplashBoss = false;

    @Expose
    @ConfigOption(name = "Highlight Deathmites", desc = "Highlight Deathmites in Dungeons in red color.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean highlightDeathmites = true;

    @Expose
    @ConfigOption(name = "Highlight Teammates", desc = "Highlight Dungeon teammates with a glowing outline.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean highlightTeammates = true;

    @Expose
    @ConfigOption(name = "Object Hider", desc = "Hide various things in Dungeons.")
    @Accordion
    public ObjectHiderConfig objectHider = new ObjectHiderConfig();

    @Expose
    @ConfigOption(name = "Message Filter", desc = "")
    @Accordion
    public MessageFilterConfig messageFilter = new MessageFilterConfig();

    @Expose
    @ConfigOption(name = "Dungeon Copilot", desc = "")
    @Accordion
    public DungeonCopilotConfig dungeonCopilot = new DungeonCopilotConfig();

    @Expose
    @ConfigOption(name = "Party Finder", desc = "")
    @Accordion
    public PartyFinderConfig partyFinder = new PartyFinderConfig();

    @Expose
    @ConfigOption(name = "Tab List", desc = "")
    @Accordion
    public TabListConfig tabList = new TabListConfig();

    @Expose
    @ConfigOption(name = "Livid Finder", desc = "")
    @Accordion
    public LividFinderConfig lividFinder = new LividFinderConfig();

    @Expose
    @ConfigOption(name = "Moving Skeleton Skulls", desc = "Highlight Skeleton Skulls when combining into an " +
        "orange Skeletor (not useful when combined with feature Hide Skeleton Skull).")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean highlightSkeletonSkull = true;

    @Expose
    @ConfigOption(name = "Croesus Chest", desc = "Adds a visual highlight to the Croesus inventory that " +
        "shows unopened chests.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean croesusUnopenedChestTracker = true;
}
