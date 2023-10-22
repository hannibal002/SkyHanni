package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
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
    public static class CleanEndConfig {
        @Expose
        @ConfigOption(name = "Enabled", desc = "After the last Dungeon boss has died, all entities and " +
                "particles are no longer displayed and the music stops playing, but the loot chests are still displayed.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "Ignore Guardians", desc = "Ignore F3 and M3 Guardians from the clean end feature when " +
                "sneaking. Makes it easier to kill them after the boss died already. Thanks Hypixel.")
        @ConfigEditorBoolean
        public boolean F3IgnoreGuardians = false;
    }

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
    public static class ObjectHiderConfig {
        @Expose
        @ConfigOption(name = "Hide Superboom TNT", desc = "Hide Superboom TNT laying around in Dungeons.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean hideSuperboomTNT = false;

        @Expose
        @ConfigOption(name = "Hide Blessings", desc = "Hide Blessings laying around in Dungeons.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean hideBlessing = false;

        @Expose
        @ConfigOption(name = "Hide Revive Stones", desc = "Hide Revive Stones laying around in Dungeons.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean hideReviveStone = false;

        @Expose
        @ConfigOption(name = "Hide Premium Flesh", desc = "Hide Premium Flesh laying around in Dungeons.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean hidePremiumFlesh = false;

        @Expose
        @ConfigOption(name = "Hide Journal Entry", desc = "Hide Journal Entry pages laying around in Dungeons.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean hideJournalEntry = false;

        @Expose
        @ConfigOption(name = "Hide Skeleton Skull", desc = "Hide Skeleton Skulls laying around in Dungeons.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean hideSkeletonSkull = true;

        @Expose
        @ConfigOption(name = "Hide Healer Orbs", desc = "Hides the damage, ability damage and defensive orbs that spawn when the Healer kills mobs.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean hideHealerOrbs = false;

        @Expose
        @ConfigOption(name = "Hide Healer Fairy", desc = "Hide the Golden Fairy that follows the Healer in Dungeons.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean hideHealerFairy = false;

        @Expose
        @ConfigOption(
                name = "Hide Soulweaver Skulls",
                desc = "Hide the annoying soulweaver skulls that float around you if you have the soulweaver gloves equipped.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean hideSoulweaverSkulls = false;

    }

    @Expose
    @ConfigOption(name = "Message Filter", desc = "")
    @Accordion
    public MessageFilterConfig messageFilter = new MessageFilterConfig();

    public static class MessageFilterConfig{
        @Expose
        @ConfigOption(name = "Keys and Doors", desc = "Hides the chat message when picking up keys or opening doors in Dungeons.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean keysAndDoors = false;
    }

    @Expose
    @ConfigOption(name = "Dungeon Copilot", desc = "")
    @Accordion
    public DungeonCopilotConfig dungeonCopilot = new DungeonCopilotConfig();

    public static class DungeonCopilotConfig{
        @Expose
        @ConfigOption(name = "Copilot Enabled", desc = "Suggests what to do next in Dungeons.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = false;

        @Expose
        public Position pos = new Position(10, 10, false, true);
    }


    @Expose
    @ConfigOption(name = "Party Finder", desc = "")
    @Accordion
    public PartyFinderConfig partyFinder = new PartyFinderConfig();

    public static class PartyFinderConfig {
        @Expose
        @ConfigOption(name = "Colored Class Level", desc = "Color class levels in Party Finder.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean coloredClassLevel = true;

        @Expose
        @ConfigOption(name = "Floor Stack Size", desc = "Display the party finder floor as the item stack size.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean floorAsStackSize = true;

        @Expose
        @ConfigOption(name = "Mark Paid Carries", desc = "Highlight paid carries with a red background to make them easier to find/skip.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean markPaidCarries = true;

        @Expose
        @ConfigOption(name = "Mark Low Levels", desc = "Highlight groups with players at or below the specified class level to make them easier to find/skip.")
        @ConfigEditorSlider(minValue = 0, maxValue = 50, minStep = 1)
        @FeatureToggle
        public int markBelowClassLevel = 0;

        @Expose
        @ConfigOption(name = "Mark Ineligible Groups", desc = "Highlight groups with requirements that you do not meet.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean markIneligibleGroups = true;

        @Expose
        @ConfigOption(name = "Mark Missing Class", desc = "Highlight groups that don't currently have any members of your selected dungeon class.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean markMissingClass = true;
    }

    @Expose
    @ConfigOption(name = "Tab List", desc = "")
    @Accordion
    public TabListConfig tabList = new TabListConfig();

    public static class TabListConfig {

        @Expose
        @ConfigOption(name = "Colored Class Level", desc = "Color class levels in tab list. (Also hides rank colors and emblems, because who needs that in Dungeons anyway?)")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean coloredClassLevel = true;
    }

    @Expose
    @ConfigOption(name = "Livid Finder", desc = "")
    @Accordion
    public LividFinderConfig lividFinder = new LividFinderConfig();

    public static class LividFinderConfig {

        @Expose
        @ConfigOption(name = "Enabled", desc = "Helps find the correct livid in F5 and in M5.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "Hide Wrong Livids", desc = "Hide wrong livids entirely.")
        @ConfigEditorBoolean
        public boolean hideWrong = false;
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
