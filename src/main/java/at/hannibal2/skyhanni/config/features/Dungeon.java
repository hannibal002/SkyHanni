package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.gui.core.config.Position;
import at.hannibal2.skyhanni.config.gui.core.config.annotations.ConfigEditorBoolean;
import at.hannibal2.skyhanni.config.gui.core.config.annotations.ConfigEditorButton;
import at.hannibal2.skyhanni.config.gui.core.config.annotations.ConfigOption;
import com.google.gson.annotations.Expose;

public class Dungeon {

    @Expose
    @ConfigOption(name = "Clicked Blocks", desc = "Highlight the following blocks when clicked in dungeon: Lever, Chest, Wither Essence")
    @ConfigEditorBoolean
    public boolean highlightClickedBlocks = false;

    @Expose
    @ConfigOption(name = "Boss Damage Indicator", desc = "Show the missing health of a boss in the dungeon and the cooldown time until the boss becomes attackable.")
    @ConfigEditorBoolean
    public boolean bossDamageIndicator = false;

    @Expose
    @ConfigOption(name = "Milestone Display", desc = "Show the current milestone inside Dungeons.")
    @ConfigEditorBoolean
    public boolean showMilestoneDisplay = false;

    @Expose
    @ConfigOption(name = "Milestone Display Position", desc = "")
    @ConfigEditorButton(runnableId = "dungeonMilestoneDisplay", buttonText = "Edit")
    public Position milestoneDisplayPos = new Position(10, 10, false, true);

    @Expose
    @ConfigOption(name = "Death Counter", desc = "Display the total amount of deaths in the current dungeon.")
    @ConfigEditorBoolean
    public boolean deathCounter = false;

    @Expose
    @ConfigOption(name = "Death Counter Position", desc = "")
    @ConfigEditorButton(runnableId = "dungeonDeathCounter", buttonText = "Edit")
    public Position deathCounterPos = new Position(10, 10, false, true);

    @Expose
    @ConfigOption(name = "Clean End", desc = "Hide entities and particles after the boss in Floor 1 - 6 has died.")
    @ConfigEditorBoolean
    public boolean cleanEnd = false;

    @Expose
    @ConfigOption(name = "Ignore Guardians", desc = "Ignore F3 and M3 guardians from the clean end feature when sneaking. Makes it easier to kill them after the boss died already. Thanks hypixel.")
    @ConfigEditorBoolean
    public boolean cleanEndF3IgnoreGuardians = false;
}