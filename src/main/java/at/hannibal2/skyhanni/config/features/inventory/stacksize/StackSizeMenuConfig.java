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
        name = "§aBingo Card§7",
        desc = "Showing various pieces of information as a stack size for these menu items.\nSome values may be truncated percentages or §a✔§r§7s.\n§c§lWARNING§r§c: If you have any respect for your FPS, please §l§odon't§r§c enable §l§oeverything§r§c at once."
    )
    @ConfigEditorDraggableList
    public List<Bingo> bingo = new ArrayList<>(Arrays.asList(
        Bingo.SECRET_BINGO_DISCOVERY,
        Bingo.TOP_BLANK_PERCENT_COMMUNITY_GOAL_CONTRIB
    ));

    public enum Bingo {
        SECRET_BINGO_DISCOVERY("§bSecret Bingo Goal Discovery"), // (#, caps at 10k)
        ROW_COLUMN_DIAGONAL_PROGRESS("§bRow/Diagonal/Column Progress"), // (§a✔§b/§c§l✖§b)
        TOP_BLANK_PERCENT_COMMUNITY_GOAL_CONTRIB("§bCommunity Goals Percent Position"), //(#)
        ;

        final String str;
        Bingo(String str) {
            this.str = str;
        }
        @Override public String toString() {
            return str;
        }
    }
}
