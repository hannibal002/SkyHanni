package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.gui.core.config.annotations.ConfigEditorBoolean;
import at.hannibal2.skyhanni.config.gui.core.config.annotations.ConfigEditorDraggableList;
import at.hannibal2.skyhanni.config.gui.core.config.annotations.ConfigEditorDropdown;
import at.hannibal2.skyhanni.config.gui.core.config.annotations.ConfigOption;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DamageIndicator {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Show the missing health of a boss.")
    @ConfigEditorBoolean
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Healing Chat Message", desc = "Sends a chat message when a boss heals himself.")
    @ConfigEditorBoolean
    public boolean healingMessage = false;

    @Expose
    @ConfigOption(
            name = "Boss Name",
            desc = "Change how the boss name should be displayed")
    @ConfigEditorDropdown(values = {"Disabled", "Full Name", "Short Name"})
    public int bossName = 1;

    @Expose
    @ConfigOption(
            name = "Select Boss",
            desc = "Change what type of boss you want the damage indicator be enabled for."
    )
    @ConfigEditorDraggableList(
            exampleText = {
                    "\u00a7bDungeon All",
                    "\u00a7bNether Mini Bosses",
                    "\u00a7bVanquisher",
                    "\u00a7bEndstone Protector (not tested)",
                    "\u00a7bEnder Dragon (not finished)",
                    "\u00a7bRevenant Horror",
                    "\u00a7bTarantula Broodfather",
                    "\u00a7bSven Packmaster",
                    "\u00a7bVoidgloom Seraph",
                    "\u00a7bInferno Demonlord (only tier 1 yet)",
                    "\u00a7bHeadless Horseman (bugged)",
                    "\u00a7bDungeon Floor 1",
                    "\u00a7bDungeon Floor 2",
                    "\u00a7bDungeon Floor 3",
                    "\u00a7bDungeon Floor 4",
                    "\u00a7bDungeon Floor 5",
                    "\u00a7bDungeon Floor 6",
                    "\u00a7bDungeon Floor 7"
            }
    )
    //TODO only show currently working and tested features
    public List<Integer> bossesToShow = new ArrayList<>(Arrays.asList(0, 1, 2, 5, 6, 7, 8, 9));

    @Expose
    @ConfigOption(name = "Hide Damage Splash", desc = "Hiding damage splashes near the damage indicator")
    @ConfigEditorBoolean
    public boolean hideDamageSplash = false;

    @Expose
    @ConfigOption(name = "Damage Over Time", desc = "Show damage and health over time below the damage indicator")
    @ConfigEditorBoolean
    public boolean showDamageOverTime = false;
}
