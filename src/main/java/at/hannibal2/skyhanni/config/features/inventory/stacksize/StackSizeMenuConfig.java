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
        name = "§aFarming§7",
        desc = "Showing various pieces of information as a stack size for these menu items.\nSome values may be truncated percentages or §a✔§r§7s.\n§c§lWARNING§r§c: If you have any respect for your FPS, please §l§odon't§r§c enable §l§oeverything§r§c at once."
    )
    @ConfigEditorDraggableList
    public List<Farming> farming = new ArrayList<>(Arrays.asList(
        Farming.JACOBS_MEDALS,
        Farming.VISITOR_MILESTONES,
        Farming.COMPOSTER_INSERT_ABBV
    ));

    public enum Farming {
        JACOBS_MEDALS("§bFarming Contests Medal Inventory"), // (§6§lG§r§f§lS§r§c§lB§r§b)
        VISITORS_LOGBOOK_COUNTDOWN("§bVisitor's Logbook Countdown"), // (#, highest unit of time only)
        VISITOR_MILESTONES("§bVisitor Milestones Progress"), // (%)
        VISITOR_NPC_RARITIES("§bGarden Visitor's Logbook NPC Rarities"), // (Abbv)
        COMPOSTER_INSERT_ABBV("§bComposter \"Insert from \" Counts"), // (Abbv)
        ;

        final String str;
        Farming(String str) { this.str = str; }
        @Override
        public String toString() { return str;  }
    }
}
