package at.hannibal2.skyhanni.config.features;

import io.github.moulberry.moulconfig.annotations.*;

public class ItemAbilities {

    @ConfigOption(name = "Ability Cooldown", desc = "Show the cooldown of item abilities.")
    @ConfigEditorBoolean
    public boolean itemAbilityCooldown = false;

    @ConfigOption(name = "Ability Cooldown Background", desc = "Show the cooldown color of item abilities in the background.")
    @ConfigEditorBoolean
    public boolean itemAbilityCooldownBackground = false;

    @ConfigOption(name = "Fire Veil", desc = "")
    @ConfigEditorAccordion(id = 1)
    public boolean fireVeilWand = false;

    @ConfigOption(name = "Fire Veil Design", desc = "Changes the flame particles of the Fire Veil Wand ability.")
    @ConfigEditorDropdown(values = {"Particles", "Line", "Off"})
    @ConfigAccordionId(id = 1)
    public int fireVeilWandDisplay = 0;

    @ConfigOption(
            name = "Line Color",
            desc = "Changes the color of the Fire Veil Wand line."
    )
    @ConfigEditorColour
    @ConfigAccordionId(id = 1)
    public String fireVeilWandDisplayColor = "0:245:255:85:85";
}
