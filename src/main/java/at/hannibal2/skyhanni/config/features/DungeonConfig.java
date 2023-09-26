package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.*;

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

    @ConfigOption(name = "Clean End", desc = "")
    @ConfigEditorAccordion(id = 2)
    public boolean cleanEnd = false;

    @Expose
    @ConfigOption(name = "Clean Ending", desc = "After the last dungeon boss has died, all entities and " +
            "particles are no longer displayed and the music stops playing, but the loot chests are still displayed.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    @FeatureToggle
    public boolean cleanEndToggle = false;

    @Expose
    @ConfigOption(name = "Ignore Guardians", desc = "Ignore F3 and M3 guardians from the clean end feature when " +
            "sneaking. Makes it easier to kill them after the boss died already. Thanks Hypixel.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean cleanEndF3IgnoreGuardians = false;

    @Expose
    @ConfigOption(name = "Boss Damage Splash", desc = "Hides damage splashes while inside the boss room (fixes a Skytils feature).")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean damageSplashBoss = false;

    @Expose
    @ConfigOption(name = "Highlight Deathmites", desc = "Highlight deathmites in Dungeon in red color.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean highlightDeathmites = true;

    @Expose
    @ConfigOption(name = "Highlight Teammates", desc = "Highlight dungeon teammates with a glowing outline.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean highlightTeammates = true;


    @ConfigOption(name = "Object Hider", desc = "Hide various things in Dungeons.")
    @ConfigEditorAccordion(id = 3)
    public boolean objectHider = false;

    @Expose
    @ConfigOption(name = "Hide Superboom TNT", desc = "Hide Superboom TNT laying around in Dungeons.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    @FeatureToggle
    public boolean hideSuperboomTNT = false;

    @Expose
    @ConfigOption(name = "Hide Blessings", desc = "Hide Blessings laying around in Dungeons.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    @FeatureToggle
    public boolean hideBlessing = false;

    @Expose
    @ConfigOption(name = "Hide Revive Stones", desc = "Hide Revive Stones laying around in Dungeons.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    @FeatureToggle
    public boolean hideReviveStone = false;

    @Expose
    @ConfigOption(name = "Hide Premium Flesh", desc = "Hide Premium Flesh laying around in Dungeons.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    @FeatureToggle
    public boolean hidePremiumFlesh = false;

    @Expose
    @ConfigOption(name = "Hide Journal Entry", desc = "Hide Journal Entry pages laying around in Dungeons.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    @FeatureToggle
    public boolean hideJournalEntry = false;

    @Expose
    @ConfigOption(name = "Hide Skeleton Skull", desc = "Hide Skeleton Skulls laying around in Dungeons.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    @FeatureToggle
    public boolean hideSkeletonSkull = true;

    @Expose
    @ConfigOption(name = "Hide Healer Orbs", desc = "Hides the damage, ability damage and defensive orbs that spawn when the healer kills mobs.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    @FeatureToggle
    public boolean hideHealerOrbs = false;

    @Expose
    @ConfigOption(name = "Hide Healer Fairy", desc = "Hide the golden fairy that follows the Healer in Dungeons.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    @FeatureToggle
    public boolean hideHealerFairy = false;

    @ConfigOption(name = "Message Filter", desc = "")
    @ConfigEditorAccordion(id = 4)
    public boolean messageFilter = false;

    @Expose
    @ConfigOption(name = "Keys and Doors", desc = "Hides the chat message when picking up keys or opening doors in Dungeons.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 4)
    @FeatureToggle
    public boolean messageFilterKeysAndDoors = false;

    @ConfigOption(name = "Dungeon Copilot", desc = "")
    @ConfigEditorAccordion(id = 5)
    public boolean dungeonCopilot = false;

    @Expose
    @ConfigOption(name = "Copilot Enabled", desc = "Suggests what to do next in Dungeons.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 5)
    @FeatureToggle
    public boolean copilotEnabled = false;

    @Expose
    public Position copilotPos = new Position(10, 10, false, true);

    @ConfigOption(name = "Party Finder", desc = "")
    @ConfigEditorAccordion(id = 6)
    public boolean partyFinder = false;

    @Expose
    @ConfigOption(name = "Colored Class Level", desc = "Color class levels in Party Finder.")
    @ConfigAccordionId(id = 6)
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean partyFinderColoredClassLevel = true;

    @Expose
    @ConfigOption(name = "Tab List", desc = "")
    @Accordion
    public TabListConfig tabList = new TabListConfig();

    public static class TabListConfig {

        @Expose
        @ConfigOption(name = "Colored Class Level", desc = "Color class levels in tab list.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean coloredClassLevel = true;
    }

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
