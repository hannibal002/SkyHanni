package at.hannibal2.skyhanni.config.features;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigAccordionId;
import io.github.moulberry.moulconfig.annotations.ConfigEditorAccordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorColour;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class ItemAbilityConfig {

    @Expose
    @ConfigOption(name = "Ability Cooldown", desc = "Show the cooldown of item abilities.")
    @ConfigEditorBoolean
    public boolean itemAbilityCooldown = false;

    @Expose
    @ConfigOption(name = "Ability Cooldown Background", desc = "Show the cooldown color of item abilities in the background.")
    @ConfigEditorBoolean
    public boolean itemAbilityCooldownBackground = false;

    @Expose
    @ConfigOption(name = "Fire Veil", desc = "")
    @ConfigEditorAccordion(id = 1)
    public boolean fireVeilWand = false;

    @Expose
    @ConfigOption(name = "Fire Veil Design", desc = "Changes the flame particles of the Fire Veil Wand ability.")
    @ConfigEditorDropdown(values = {"Particles", "Line", "Off"})
    @ConfigAccordionId(id = 1)
    public int fireVeilWandDisplay = 0;

    @Expose
    @ConfigOption(
            name = "Line Color",
            desc = "Changes the color of the Fire Veil Wand line."
    )
    @ConfigEditorColour
    @ConfigAccordionId(id = 1)
    public String fireVeilWandDisplayColor = "0:245:255:85:85";
}
