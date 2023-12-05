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
        name = "General",
        desc = "Showing various pieces of information as a stack size for these menu items.\nSome values may be truncated percentages or §a✔§r§7s.\n§c§lWARNING§r§c: If you have any respect for your FPS, please §l§odon't§r§c enable §l§oeverything§r§c at once."
    )
    @ConfigEditorDraggableList
    public List<PlayerGeneral> player = new ArrayList<>(Arrays.asList(
        PlayerGeneral.SKYBLOCK_LEVEL,
        PlayerGeneral.SKILL_GARDEN_DUNGEON_LEVELS,
        PlayerGeneral.SKILL_AVERAGE,
        PlayerGeneral.COLLECTION_LEVELS_AND_PROGRESS,
        PlayerGeneral.CRAFTED_MINIONS,
        PlayerGeneral.MUSEUM_PROGRESS,
        PlayerGeneral.PET_SCORE_STATUS,
        PlayerGeneral.ESSENCE_COUNTS
    ));

    public enum PlayerGeneral {
        SKYBLOCK_LEVEL("§bSkyblock Level"), //(#, won't work in Rift)
        SKILL_GARDEN_DUNGEON_LEVELS("§bSkill + Garden + Dungeoneering Levels"),
        SKILL_AVERAGE("§bSkill Average"),
        COLLECTION_LEVELS_AND_PROGRESS("§bCollection Level + Progress"), // (%)
        CRAFTED_MINIONS("§bHighest Crafted Minion + Next Minion Progress"),
        MUSEUM_PROGRESS("§bMuseum Donation Progress"), // (%, # for Special Items)
        PROFILE_ICON("§bSkyblock Profile Type Icon"),
        PET_SCORE_STATUS("§bPet Score + No Pet Indicator"), // (c§l✖§b)
        ESSENCE_COUNTS("§bEssence Counts"), // §b(# in Rewards Chests, Abbv in Essence Shops)
        MINION_QUICK_UPGRADE("§bQuick Upgrade Missing Count"),
        ;

        final String str;
        PlayerGeneral(String str) { this.str = str; }
        @Override
        public String toString() {return str; }
    }
}
