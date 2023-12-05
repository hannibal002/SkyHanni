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
        name = "§aMining§7",
        desc = "Showing various pieces of information as a stack size for these menu items.\nSome values may be truncated percentages or §a✔§r§7s.\n§c§lWARNING§r§c: If you have any respect for your FPS, please §l§odon't§r§c enable §l§oeverything§r§c at once."
    )
    @ConfigEditorDraggableList
    public List<Mining> mining = new ArrayList<>(Arrays.asList(
        Mining.HOTM_PERK_LEVELS,
        Mining.HOTM_OVERALL_TIERS
    ));

    public enum Mining {
        CURRENT_SKYMALL_PERK("§bCurrent Sky Mall Perk (Abbv)"), //do not move this PLEASE otherwise one of the other stack size features will break
        HOTM_PERK_LEVELS("§bHeart of the Mountain Perk Levels"), // (#, §c#§b when disabled)
        HOTM_OVERALL_TIERS("§bHOTM Tiers Progress"), // (%)
        CRYSTAL_HOLLOWS_NUCLEUS("§bCrystal Hollows Crystal Progress"),
        ; // (§aF§eNP§cNF§b)

        final String str;
        Mining(String str) { this.str = str; }
        @Override
        public String toString() { return str; }
    }
}
