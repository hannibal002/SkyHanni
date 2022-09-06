package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.gui.core.config.Position;
import at.hannibal2.skyhanni.config.gui.core.config.annotations.*;
import com.google.gson.annotations.Expose;

public class Dungeon {

    @Expose
    @ConfigOption(name = "Clicked Blocks", desc = "Highlight the following blocks when clicked in dungeon: Lever, Chest, Wither Essence.")
    @ConfigEditorBoolean
    public boolean highlightClickedBlocks = false;

    @ConfigOption(name = "Milestones", desc = "")
    @ConfigEditorAccordion(id = 0)
    public boolean showMilestone = false;

    @Expose
    @ConfigOption(name = "Milestones Display", desc = "Show the current milestone in the Dungeon.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean showMilestonesDisplay = false;

    @Expose
    @ConfigOption(name = "Milestone Display Position", desc = "")
    @ConfigEditorButton(runnableId = "dungeonMilestonesDisplay", buttonText = "Edit")
    @ConfigAccordionId(id = 0)
    public Position showMileStonesDisplayPos = new Position(10, 10, false, true);

    @ConfigOption(name = "Death Counter", desc = "")
    @ConfigEditorAccordion(id = 1)
    public boolean deathCounter = false;

    @Expose
    @ConfigOption(name = "Death Counter Display", desc = "Display the total amount of deaths in the current dungeon.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean deathCounterDisplay = false;

    @Expose
    @ConfigOption(name = "Death Counter Position", desc = "")
    @ConfigEditorButton(runnableId = "dungeonDeathCounter", buttonText = "Edit")
    @ConfigAccordionId(id = 1)
    public Position deathCounterPos = new Position(10, 10, false, true);

    @ConfigOption(name = "Clean End", desc = "")
    @ConfigEditorAccordion(id = 2)
    public boolean cleanEnd = false;

    @Expose
    @ConfigOption(name = "Clean End", desc = "Hide entities and particles after the boss in Floor 1 - 6 has died.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean cleanEndToggle = false;

    @Expose
    @ConfigOption(name = "Ignore Guardians", desc = "Ignore F3 and M3 guardians from the clean end feature when " +
            "sneaking. Makes it easier to kill them after the boss died already. Thanks hypixel.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean cleanEndF3IgnoreGuardians = false;

    @Expose
    @ConfigOption(name = "Boss Damage Splash", desc = "Hiding damage splashes while inside the boss room. (fixing Skytils feature)")
    @ConfigEditorBoolean
    public boolean damageSplashBoss = false;

    @Expose
    @ConfigOption(name = "Highlight Deathmites", desc = "Highlight deathmites in dungeon in red color.")
    @ConfigEditorBoolean
    public boolean highlightDeathmites = false;

    @ConfigOption(name = "Item Hider", desc = "")
    @ConfigEditorAccordion(id = 3)
    public boolean itemHider = false;

    @Expose
    @ConfigOption(name = "Hide Superboom TNT", desc = "Hide Superboom TNT laying around in dungeon.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    public boolean hideSuperboomTNT = false;

    @Expose
    @ConfigOption(name = "Hide Blessings", desc = "Hide Blessings laying around in dungeon.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    public boolean hideBlessing = false;

    @Expose
    @ConfigOption(name = "Hide Revive Stones", desc = "Hide Revive Stones laying around in dungeon.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    public boolean hideReviveStone = false;

    @Expose
    @ConfigOption(name = "Hide Premium Flesh", desc = "Hide Premium Flesh laying around in dungeon.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    public boolean hidePremiumFlesh = false;

    @Expose
    @ConfigOption(name = "Hide Journal Entry", desc = "Hide Journal Entry pages laying around in dungeon.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    public boolean hideJournalEntry = false;

    @Expose
    @ConfigOption(name = "Hide Skeleton Skull", desc = "Hide Skeleton Skulls laying around in dungeon.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    public boolean hideSkeletonSkull = false;

    @ConfigOption(name = "Message Filter", desc = "")
    @ConfigEditorAccordion(id = 4)
    public boolean messageFilter = false;

    @Expose
    @ConfigOption(name = "Keys and Doors", desc = "Hides the chat message when picking up keys or opening doors in dungeon.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 4)
    public boolean messageFilterKeysAndDoors = false;

    @ConfigOption(name = "Dungeon Copilot", desc = "")
    @ConfigEditorAccordion(id = 5)
    public boolean dungeonCopilot = false;

    @Expose
    @ConfigOption(name = "Copilot Enabled", desc = "Suggests what to do next in dungeon.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 5)
    public boolean copilotEnabled = false;

    @Expose
    @ConfigOption(name = "Copilot Pos", desc = "")
    @ConfigEditorButton(runnableId = "dungeonCopilot", buttonText = "Edit")
    @ConfigAccordionId(id = 5)
    public Position copilotPos = new Position(10, 10, false, true);

    @Expose
    @ConfigOption(name = "Show Building Skeleton Skulls", desc = "Highlight Skeleton Skulls when combining into a " +
            "skeleton in orange color (not useful combined with feature Hide Skeleton Skull)")
    @ConfigEditorBoolean
    public boolean highlightSkeletonSkull = false;
}