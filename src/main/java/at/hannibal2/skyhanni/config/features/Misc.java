package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.gui.core.config.Position;
import at.hannibal2.skyhanni.config.gui.core.config.annotations.*;
import com.google.gson.annotations.Expose;

public class Misc {

    @Expose
    @ConfigOption(name = "Damage Indicator", desc = "")
    @ConfigEditorAccordion(id = 1)
    public boolean damageIndicatorInternal = false;

    @Expose
    @ConfigOption(name = "Enabled", desc = "Show the missing health of a boss.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean damageIndicator = false;

    @Expose
    @ConfigOption(name = "Healing Chat Message", desc = "Sends a chat message when a boss heals himself.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean damageIndicatorHealingMessage = false;

    @Expose
    @ConfigOption(
            name = "Boss Name",
            desc = "Change how the boss name should be displayed")
    @ConfigEditorDropdown(values = {"Disabled", "Full Name", "Short Name"})
    @ConfigAccordionId(id = 1)
    public int damageIndicatorBossName = 0;

    @Expose
    @ConfigOption(name = "Pet Display", desc = "Show the currently active pet.")
    @ConfigEditorBoolean
    public boolean petDisplay = false;

    @Expose
    @ConfigOption(name = "Pet Display Position", desc = "")
    @ConfigEditorButton(runnableId = "petDisplay", buttonText = "Edit")
    public Position petDisplayPos = new Position(10, 10, false, true);

    @Expose
    @ConfigOption(name = "Exp Bottles", desc = "Hides all the experience bottles lying on the ground.")
    @ConfigEditorBoolean
    public boolean hideExpBottles = false;

    @Expose
    @ConfigOption(name = "Summon Soul Display", desc = "Shows the name above summoning souls that ready to pick up. §cNot working in Dungeon if Skytils' 'Hide Non-Starred Mobs Nametags' feature is enabled!")
    @ConfigEditorBoolean
    public boolean summonSoulDisplay = false;

    @Expose
    @ConfigOption(name = "Config Button", desc = "Add a button to the pause menu to configure SkyHanni.")
    @ConfigEditorBoolean
    public boolean configButtonOnPause = true;
}