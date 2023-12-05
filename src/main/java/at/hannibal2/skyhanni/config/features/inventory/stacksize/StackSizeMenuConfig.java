package at.hannibal2.skyhanni.config.features.inventory.stacksize;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StackSizeMenuConfig {

    @Expose
    @ConfigOption(
        name = "§aCombat§7",
        desc = "Showing various pieces of information as a stack size for these menu items.\nSome values may be truncated percentages or §a✔§r§7s.\n§c§lWARNING§r§c: If you have any respect for your FPS, please §l§odon't§r§c enable §l§oeverything§r§c at once."
    )
    @ConfigEditorDraggableList
    public List<Combat> combat = new ArrayList<>(Arrays.asList(
        Combat.BESTIARY_LEVEL,
        Combat.BESTIARY_OVERALL_FAMILY_PROGRESS,
        Combat.SLAYER_LEVELS
    ));

    public enum Combat {
        BESTIARY_LEVEL("§bBestiary Level"), //(#)
        BESTIARY_OVERALL_FAMILY_PROGRESS("§bBestiary Progress"), // (%, Overall + per Family)
        SLAYER_LEVELS("§bCurrent Slayer Levels"), // (#)
        SLAYER_COMBAT_WISDOM_BUFF("§bSlayer Combat Wisdom Buff"), // (#)
        RNG_METER_PROGRESS("§bSlayer/Catacombs RNG Meter Progress"), // (%)
        UNLOCKED_SLAYER_RECIPES("§bUnlocked Slayer Recipes"),
        ; // (#)

        final String str;
        Combat(String str) { this.str = str; }
        @Override public String toString() { return str; }
    }

}
