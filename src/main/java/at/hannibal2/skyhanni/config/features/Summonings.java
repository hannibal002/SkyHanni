package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.config.core.config.annotations.*;
import com.google.gson.annotations.Expose;

public class Summonings {

    @Expose
    @ConfigOption(name = "Summoning Soul Display", desc = "Show the name of dropped summoning souls laying on the ground. " +
            "§cNot working in dungeons if Skytils' 'Hide Non-Starred Mobs Nametags' feature is enabled!")
    @ConfigEditorBoolean
    public boolean summoningSoulDisplay = false;

    @Expose
    @ConfigOption(name = "Summoning Mob", desc = "")
    @ConfigEditorAccordion(id = 0)
    public boolean summoningMob = false;

    @Expose
    @ConfigOption(name = "Summoning Mob Display", desc = "Show the health of your spawned summons.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean summoningMobDisplay = false;

    @Expose
    @ConfigOption(name = "Summoning Mob Display Position", desc = "")
    @ConfigEditorButton(runnableId = "summoningMobDisplay", buttonText = "Edit")
    @ConfigAccordionId(id = 0)
    public Position summoningMobDisplayPos = new Position(10, 10, false, true);

    @Expose
    @ConfigOption(name = "Summoning Mob Nametag", desc = "Hide the nametag of your spawned summons.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean summoningMobHideNametag = false;

    @Expose
    @ConfigOption(name = "Summoning Mob Color", desc = "Marks own summons green.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean summoningMobColored = false;
}
