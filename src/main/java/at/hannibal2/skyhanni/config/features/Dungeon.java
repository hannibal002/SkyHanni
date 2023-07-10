package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.Position;
import io.github.moulberry.moulconfig.annotations.ConfigAccordionId;
import io.github.moulberry.moulconfig.annotations.ConfigEditorAccordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class Dungeon {

    @ConfigOption(name = "Clicked Blocks", desc = "Highlight levers, chests, and wither essence when clicked in dungeons.")
    @ConfigEditorBoolean
    public boolean highlightClickedBlocks = false;

    @ConfigOption(name = "Milestones Display", desc = "Show the current milestone in dungeons.")
    @ConfigEditorBoolean
    public boolean showMilestonesDisplay = false;

    public Position showMileStonesDisplayPos = new Position(10, 10, false, true);

    @ConfigOption(name = "Death Counter Display", desc = "Display the total amount of deaths in the current dungeon.")
    @ConfigEditorBoolean
    public boolean deathCounterDisplay = false;

    public Position deathCounterPos = new Position(10, 10, false, true);

    @ConfigOption(name = "Clean End", desc = "")
    @ConfigEditorAccordion(id = 2)
    public boolean cleanEnd = false;

    @ConfigOption(name = "Clean Ending", desc = "After the last dungeon boss has died, all entities and " +
            "particles are no longer displayed and the music stops playing, but the loot chests are still displayed.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean cleanEndToggle = false;

    @ConfigOption(name = "Ignore Guardians", desc = "Ignore F3 and M3 guardians from the clean end feature when " +
            "sneaking. Makes it easier to kill them after the boss died already. Thanks Hypixel.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean cleanEndF3IgnoreGuardians = false;

    @ConfigOption(name = "Boss Damage Splash", desc = "Hides damage splashes while inside the boss room (fixes a Skytils feature).")
    @ConfigEditorBoolean
    public boolean damageSplashBoss = false;

    @ConfigOption(name = "Highlight Deathmites", desc = "Highlight deathmites in dungeon in red color.")
    @ConfigEditorBoolean
    public boolean highlightDeathmites = true;

    @ConfigOption(name = "Object Hider", desc = "Hide various things in dungeons.")
    @ConfigEditorAccordion(id = 3)
    public boolean objectHider = false;

    @ConfigOption(name = "Hide Superboom TNT", desc = "Hide Superboom TNT laying around in dungeons.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    public boolean hideSuperboomTNT = false;

    @ConfigOption(name = "Hide Blessings", desc = "Hide Blessings laying around in dungeons.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    public boolean hideBlessing = false;

    @ConfigOption(name = "Hide Revive Stones", desc = "Hide Revive Stones laying around in dungeons.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    public boolean hideReviveStone = false;

    @ConfigOption(name = "Hide Premium Flesh", desc = "Hide Premium Flesh laying around in dungeons.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    public boolean hidePremiumFlesh = false;

    @ConfigOption(name = "Hide Journal Entry", desc = "Hide Journal Entry pages laying around in dungeons.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    public boolean hideJournalEntry = false;

    @ConfigOption(name = "Hide Skeleton Skull", desc = "Hide Skeleton Skulls laying around in dungeons.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    public boolean hideSkeletonSkull = true;

    @ConfigOption(name = "Hide Healer Orbs", desc = "Hides the damage, ability damage and defensive orbs that spawn when the healer kills mobs.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    public boolean hideHealerOrbs = false;

    @ConfigOption(name = "Hide Healer Fairy", desc = "Hide the golden fairy that follows the healer in dungeons.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    public boolean hideHealerFairy = false;

    @ConfigOption(name = "Message Filter", desc = "")
    @ConfigEditorAccordion(id = 4)
    public boolean messageFilter = false;

    @ConfigOption(name = "Keys and Doors", desc = "Hides the chat message when picking up keys or opening doors in dungeons.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 4)
    public boolean messageFilterKeysAndDoors = false;

    @ConfigOption(name = "Dungeon Copilot", desc = "")
    @ConfigEditorAccordion(id = 5)
    public boolean dungeonCopilot = false;

    @ConfigOption(name = "Copilot Enabled", desc = "Suggests what to do next in dungeons.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 5)
    public boolean copilotEnabled = false;

    public Position copilotPos = new Position(10, 10, false, true);

    @ConfigOption(name = "Party Finder", desc = "")
    @ConfigEditorAccordion(id = 6)
    public boolean partyFinder = false;

    @ConfigOption(name = "Colored Class Level", desc = "Color class levels in party finder.")
    @ConfigAccordionId(id = 6)
    @ConfigEditorBoolean
    public boolean partyFinderColoredClassLevel = true;

    @ConfigOption(name = "Moving Skeleton Skulls", desc = "Highlight Skeleton Skulls when combining into an " +
            "orange Skeletor (not useful when combined with feature Hide Skeleton Skull).")
    @ConfigEditorBoolean
    public boolean highlightSkeletonSkull = true;

    @ConfigOption(name = "Croesus Unopened Chest Tracker", desc = "Adds a visual highlight to the Croesus inventory that " +
            "shows unopened chests.")
    @ConfigEditorBoolean
    public boolean croesusUnopenedChestTracker = true;
}
