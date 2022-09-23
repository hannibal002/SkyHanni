package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.config.core.config.annotations.*;
import com.google.gson.annotations.Expose;

public class Misc {

    @Expose
    @ConfigOption(name = "Pet", desc = "")
    @ConfigEditorAccordion(id = 0)
    public boolean pet = false;

    @Expose
    @ConfigOption(name = "Pet Display", desc = "Show the currently active pet.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean petDisplay = false;

    @Expose
    @ConfigOption(name = "Pet Display Position", desc = "")
    @ConfigEditorButton(runnableId = "petDisplay", buttonText = "Edit")
    @ConfigAccordionId(id = 0)
    public Position petDisplayPos = new Position(10, 10, false, true);

    @Expose
    @ConfigOption(name = "Time", desc = "")
    @ConfigEditorAccordion(id = 1)
    public boolean time = false;

    @Expose
    @ConfigOption(name = "Real Time", desc = "Show the real time. Useful while playing in full screen mode")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean realTime = false;

    @Expose
    @ConfigOption(name = "Real Time Position", desc = "")
    @ConfigEditorButton(runnableId = "realTime", buttonText = "Edit")
    @ConfigAccordionId(id = 1)
    public Position realTimePos = new Position(10, 10, false, true);

    @Expose
    @ConfigOption(name = "Highlight Mobs", desc = "")
    @ConfigEditorAccordion(id = 2)
    public boolean highlightColor = false;

    @Expose
    @ConfigOption(name = "Voidling Extremist Color", desc = "Highlight the voidling extremist in pink color")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean voidlingExtremistColor = false;

    @Expose
    @ConfigOption(name = "Corrupted Mob Highlight", desc = "Highlight corrupted mobs in purple color")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean corruptedMobHighlight = false;

    @Expose
    @ConfigOption(name = "Hide Armor", desc = "")
    @ConfigEditorAccordion(id = 3)
    public boolean hideArmor = false;

    @Expose
    @ConfigOption(name = "Hide Armor", desc = "Hide the armor of players.")
    @ConfigEditorBoolean(runnableId = "hideArmor")
    @ConfigAccordionId(id = 3)
    public boolean hideArmorEnabled = false;

    @Expose
    @ConfigOption(name = "Own Armor", desc = "Hide the armor of yourself.")
    @ConfigEditorBoolean(runnableId = "hideArmor")
    @ConfigAccordionId(id = 3)
    public boolean hideArmorOwn = true;

    @Expose
    @ConfigOption(name = "Only Helmet", desc = "Hide only the helmet.")
    @ConfigEditorBoolean(runnableId = "hideArmor")
    @ConfigAccordionId(id = 3)
    public boolean hideArmorOnlyHelmet = false;

    @Expose
    @ConfigOption(name = "Damage Splash", desc = "")
    @ConfigEditorAccordion(id = 4)
    public boolean damageSplash = false;

    @Expose
    @ConfigOption(name = "Skytils Damage Splash", desc = "Fixing the custom damage splash feature from skytils.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 4)
    public boolean fixSkytilsDamageSplash = true;

    @Expose
    @ConfigOption(name = "Hide Damage Splash", desc = "Hide all damage splashes, from anywhere in Skyblock.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 4)
    public boolean hideDamageSplash = false;

    @Expose
    @ConfigOption(name = "Exp Bottles", desc = "Hides all the experience bottles lying on the ground.")
    @ConfigEditorBoolean
    public boolean hideExpBottles = false;

    @Expose
    @ConfigOption(name = "Config Button", desc = "Add a button to the pause menu to configure SkyHanni.")
    @ConfigEditorBoolean
    public boolean configButtonOnPause = true;
}