package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.Position;
import io.github.moulberry.moulconfig.annotations.ConfigAccordionId;
import io.github.moulberry.moulconfig.annotations.ConfigEditorAccordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class Summonings {

    @ConfigOption(name = "Summoning Soul Display", desc = "Show the name of dropped summoning souls laying on the ground. " +
            "§cNot working in dungeons if Skytils' 'Hide Non-Starred Mobs Nametags' feature is enabled!")
    @ConfigEditorBoolean
    public boolean summoningSoulDisplay = false;

    @ConfigOption(name = "Summoning Mob", desc = "")
    @ConfigEditorAccordion(id = 0)
    public boolean summoningMob = false;

    @ConfigOption(name = "Summoning Mob Display", desc = "Show the health of your spawned summons.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean summoningMobDisplay = false;

    public Position summoningMobDisplayPos = new Position(10, 10, false, true);

    @ConfigOption(name = "Summoning Mob Nametag", desc = "Hide the nametag of your spawned summons.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean summoningMobHideNametag = false;

    @ConfigOption(name = "Summoning Mob Color", desc = "Marks own summons green.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean summoningMobColored = false;
}
