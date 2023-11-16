package at.hannibal2.skyhanni.config.features.itemability;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorColour;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class FireVeilWandConfig {
    @Expose
    @ConfigOption(name = "Fire Veil Design", desc = "Changes the flame particles of the Fire Veil Wand ability.")
    @ConfigEditorDropdown(values = {"Particles", "Line", "Off"})
    public int display = 0;

    @Expose
    @ConfigOption(
        name = "Line Color",
        desc = "Changes the color of the Fire Veil Wand line."
    )
    @ConfigEditorColour
    public String displayColor = "0:245:255:85:85";
}
