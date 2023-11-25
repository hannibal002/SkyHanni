package at.hannibal2.skyhanni.config.features.inventory.stacksize;

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

    public enum ItemNumber {
        MASTER_STAR("§bMaster Star Tier"),
        MASTER_SKULL("§bMaster Skull Tier"),
        DUNGEON_HEAD_FLOOR_NUMBER("§bGolden/Diamond Dungeon Head Floor Number"),
        SKYBLOCK_YEAR("§bNew Year Cake/Spooky Pie SB Year"),
        PET_LVL("§bPet Level"),
        MINION_TIER("§bMinion Tier"),
        CRIMSON_STARS("§bCrimson Armor Crimson Stars"),
        KUUDRA("§bKuudra Key"),
        RANCHER_SPEED("§bRancher's Boots Speed"),
        LARVA_HOOK("§bLarva Hook"),
        DUNGEON_POTION_LEVEL("§bDungeon Potion Level"),
        ARMADILLO("§bArmadillo Blocks Walked Progress (%)"),
        NECRONS_LADDER("§bNecron's Ladder Progress"),
        FRUIT_BOWL("§bFruit Bowl Progress"),
        BEASTMASTER("§bBeastmaster Crest Kill Progress (%)"),
        CAMPFIRE("§bCampfire Talisman Tier"),
        BLOOD_GOD("§bBlood God Crest Strength"),
        YETI_ROD("§bYeti Rod Bonus"),
        SHREDDER("§bShredder Bonus Damage"),
        JYRRE("§bBottle of Jyrre Intelligence Bonus"),
        SOULFLOW("§bInternalized Soulflow Count\n§b(Abbv, won't show in the Auction House)"),
        CRUX("§bCrux Accessory Kill Overall Progress\n§b(%, out of all mob types)"),
        STORAGE_TIER("§bMinion Storage Tier (#)"),
        COMPACTOR_DELETOR("§bCompactor/Deletor Enabled Status (§a✔§b/§c§l✖§b) + Tier (Abbv)"),
        ABIPHONE("§bAbiphone Tier"),
        EDITION_AUCTION_NUMBER("§bItem Edition/Auction Number (if less than 1000)"),
        STACKING_ENCHANTMENT("§bStacking Enchantment Tier (for items without dungeon stars)"),
        VACCUM_PESTS("§bPests Stored in Vaccum (Garden)");

        final String str;

        ItemNumber(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    @Expose
    @Accordion
    @ConfigOption(name = "Stack Size in Menus", desc = "")
    public StackSizeMenuConfig menu = new StackSizeMenuConfig();
}