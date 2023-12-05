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
        name = "§aSB Levels§7",
        desc = "Showing various pieces of information as a stack size for these menu items.\nSome values may be truncated percentages or §a✔§r§7s.\n§c§lWARNING§r§c: If you have any respect for your FPS, please §l§odon't§r§c enable §l§oeverything§r§c at once."
    )
    @ConfigEditorDraggableList
    public List<SBLeveling> sbLeveling = new ArrayList<>(Arrays.asList(
        SBLeveling.GUIDE_PROGRESS,
        SBLeveling.WAYS_TO_LEVEL_UP_PROGRESS,
        SBLeveling.SB_LEVELING_REWARDS
    ));

    public enum SBLeveling {
        GUIDE_PROGRESS("§bSkyblock Guide Progress"), //(%)
        WAYS_TO_LEVEL_UP_PROGRESS("§bSkyblock Ways To Level Up Tasks"), //(%)
        SB_LEVELING_REWARDS("§bSkyblock Leveling Rewards Progress"), //(%)
        EMBLEMS_UNLOCKED("§bEmblems Unlocked"), // (#)
        ;

        final String str;
        SBLeveling(String str) { this.str = str; }
        @Override public String toString() { return str; }
    }
}
