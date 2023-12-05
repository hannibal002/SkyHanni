package at.hannibal2.skyhanni.config.features.inventory.stacksize;

import at.hannibal2.skyhanni.config.HasLegacyId;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StackSizeConfig {

    @Expose
    @ConfigOption(
        name = "Item Number",
        desc = "Showing the item number as a stack size for these items." // Some values may be truncated percentages or §a✔§r§7s.
    )
    @ConfigEditorDraggableList
    public List<ItemNumberEntry> itemNumberAsStackSize = new ArrayList<>(Arrays.asList(
        ItemNumberEntry.MASTER_STAR_TIER,
        ItemNumberEntry.MASTER_SKULL_TIER,
        ItemNumberEntry.NEW_YEAR_CAKE,
        ItemNumberEntry.CRIMSON_ARMOR,
        ItemNumberEntry.LARVA_HOOK,
        ItemNumberEntry.ARMADILLO,
        ItemNumberEntry.BEASTMASTER,
        ItemNumberEntry.CAMPFIRE
    ));

    public enum ItemNumberEntry implements HasLegacyId {
        MASTER_STAR_TIER("§bMaster Star Tier", 0),
        MASTER_SKULL_TIER("§bMaster Skull Tier", 1),
        DUNGEON_HEAD_FLOOR_NUMBER("§bGolden/Diamond Dungeon Head Floor Number", 2),
        NEW_YEAR_CAKE("§bNew Year Cake/Spooky Pie SB Year", 3),
        PET_LEVEL("§bPet Level", 4),
        MINION_TIER("§bMinion Tier", 5),
        CRIMSON_ARMOR("§bCrimson Armor Crimson Stars", 6),
        CAMPFIRE("§bCampfire Talisman Tier", 7),
        KUUDRA_KEY("§bKuudra Key", 8),
        SOULFLOW("§bInternalized Soulflow Count", 9), //(Abbv, won't show in the Auction House)
        BLOOD_GOD("§bBlood God Crest Strength", 10),
        RANCHERS_BOOTS_SPEED("§bRancher's Boots Speed", 11),
        LARVA_HOOK("§bLarva Hook", 12),
        DUNGEON_POTION_LEVEL("§bDungeon Potion Level", 13),
        VACUUM_GARDEN("§bPests Stored in Vaccum", 14),
        BOTTLE_OF_JYRRE("§bBottle of Jyrre Intel Bonus", 15),
        EDITION_NUMBER("§bItem Edition/Auction Number (if less than 1000)", 16),
        ARMADILLO("§bArmadillo Blocks Walked Progress (%)"),
        NECRONS_LADDER("§bNecron's Ladder Progress"),
        FRUIT_BOWL("§bFruit Bowl Progress"),
        BEASTMASTER("§bBeastmaster Crest Kills"), // Progress (%)
        YETI_ROD("§bYeti Rod Bonus"),
        SHREDDER("§bShredder Bonus Damage"),
        STORAGE_TIER("§bMinion Storage Tier"), // (#)
        COMPACTOR_DELETOR("§bCompactor/Deletor Enabled Status + Tier (Abbv)"), // (§a✔§b/§c§l✖§b)
        ABIPHONE("§bAbiphone Tier", 7),
        STACKING_ENCHANTMENT("§bStacking Enchantment Tier"), // (for items without dungeon stars)
        ;

        final String str;
        final int legacyId;
        ItemNumberEntry(String str, int legacyId) { this.str = str; this.legacyId = legacyId; }
        ItemNumberEntry(String str) { this(str, -1); }
        @Override public String toString() { return str; }
        @Override public int getLegacyId() { return legacyId; }
    }

    @Expose
    @Accordion
    @ConfigOption(name = "Stack Size in Menus", desc = "")
    public StackSizeMenuConfig menu = new StackSizeMenuConfig();
}
