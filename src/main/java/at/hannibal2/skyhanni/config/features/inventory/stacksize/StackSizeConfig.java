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
    public List<ItemNumber> itemNumber = new ArrayList<>(Arrays.asList(
        ItemNumber.MASTER_STAR,
        ItemNumber.MASTER_SKULL,
        ItemNumber.SKYBLOCK_YEAR,
        ItemNumber.CRIMSON_STARS,
        ItemNumber.LARVA_HOOK,
        ItemNumber.ARMADILLO,
        ItemNumber.BEASTMASTER,
        ItemNumber.CAMPFIRE
    ));

    public enum ItemNumber implements HasLegacyId {
        MASTER_STAR("§bMaster Star Tier", 0),
        MASTER_SKULL("§bMaster Skull Tier", 1),
        DUNGEON_HEAD_FLOOR_NUMBER("§bGolden/Diamond Dungeon Head Floor Number", 2),
        SKYBLOCK_YEAR("§bNew Year Cake/Spooky Pie SB Year", 3),
        PET_LVL("§bPet Level", 4),
        MINION_TIER("§bMinion Tier", 5),
        CRIMSON_STARS("§bCrimson Armor Crimson Stars", 6),
        CAMPFIRE("§bCampfire Talisman Tier", 7),
        KUUDRA("§bKuudra Key", 8),
        SOULFLOW("§bInternalized Soulflow Count\n§b(Abbv, won't show in the Auction House)", 9),
        BLOOD_GOD("§bBlood God Crest Strength", 10),
        RANCHER_SPEED("§bRancher's Boots Speed", 11),
        LARVA_HOOK("§bLarva Hook", 12),
        DUNGEON_POTION_LEVEL("§bDungeon Potion Level", 13),
        VACCUM_PESTS("§bPests Stored in Vaccum (Garden)", 14),
        JYRRE("§bBottle of Jyrre (#, Intelligence Bonus on Legacy Bottles and Time on New Bottles)", 15),
        EDITION_AUCTION_NUMBER("§bItem Edition/Auction Number (if less than 1000)", 16),
        ARMADILLO("§bArmadillo Blocks Walked Progress (%)"),
        NECRONS_LADDER("§bNecron's Ladder Progress"),
        FRUIT_BOWL("§bFruit Bowl Progress"),
        BEASTMASTER("§bBeastmaster Crest Kill Progress (%)"),
        YETI_ROD("§bYeti Rod Bonus"),
        SHREDDER("§bShredder Bonus Damage"),
        STORAGE_TIER("§bMinion Storage Tier (#)"),
        COMPACTOR_DELETOR("§bCompactor/Deletor Enabled Status (§a✔§b/§c§l✖§b) + Tier (Abbv)"),
        ABIPHONE("§bAbiphone Tier", 7),
        STACKING_ENCHANTMENT("§bStacking Enchantment Tier (for items without dungeon stars)"),
        ;

        final String str;
        final int legacyId;
        ItemNumber(String str, int legacyId) { this.str = str; this.legacyId = legacyId; }
        ItemNumber(String str) { this(str, -1); }
        @Override public String toString() { return str; }
        @Override public int getLegacyId() { return legacyId; }
    }

    @Expose
    @Accordion
    @ConfigOption(name = "Stack Size in Menus", desc = "")
    public StackSizeMenuConfig menu = new StackSizeMenuConfig();
}
