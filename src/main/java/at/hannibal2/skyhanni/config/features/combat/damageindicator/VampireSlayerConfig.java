package at.hannibal2.skyhanni.config.features.combat.damageindicator;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class VampireSlayerConfig {

    @Expose
    @ConfigOption(name = "HP Until Steak", desc = "Show the amount of HP missing until the Steak can be used on the Vampire Slayer on top of the boss.")
    @ConfigEditorBoolean
    public boolean hpTillSteak = false;

    @Expose
    @ConfigOption(name = "Mania Circles", desc = "Show a timer until the boss leaves the invincible Mania Circles state.")
    @ConfigEditorBoolean
    public boolean maniaCircles = false;

    @Expose
    @ConfigOption(name = "Percentage HP", desc = "Show the percentage of HP next to the HP.")
    @ConfigEditorBoolean
    public boolean percentage = false;
}
