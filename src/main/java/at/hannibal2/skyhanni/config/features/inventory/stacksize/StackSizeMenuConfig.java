package at.hannibal2.skyhanni.config.features.inventory.stacksize;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StackSizeMenuConfig {
    private final String stackSizeConfigDesc = "Showing various pieces of information as a stack size for these menu items.\nSome values may be truncated percentages or §a✔§r§7s.\n§c§lWARNING§r§c: If you have any respect for your FPS, §4§l§oplease don't enable everything at once§r§c!";

    @Expose
    @ConfigOption(
        name = "Advanced",
        desc = stackSizeConfigDesc
    )
    @ConfigEditorDraggableList
    public List<PlayerAdvanced> playerAdvanced = new ArrayList<>(Arrays.asList(
        PlayerAdvanced.UNLOCKED_RECIPES,
        PlayerAdvanced.AUCTION_BAZAAR_VARIOUS,
        PlayerAdvanced.PROJECTS,
        PlayerAdvanced.BANK_UTILS,
        PlayerAdvanced.MAYOR_PERKS
    ));

    public enum PlayerAdvanced {
        UNLOCKED_RECIPES("§bUnlocked Recipes"), // (%)
        FAIRY_ENIGMA_SOULS_QUESTS("§bFairy/Enigma Souls + Completed Quests"),
        TRADES_UNLOCKED("§bTrades Unlocked"), // (%)
        WARDROBE_SLOT("§bWardrobe Slot #"),
        ABBV_STATS("§bSkyblock Stat Names"),
        CUTE_NAME("§bSkyblock Profile Fruit Name"),
        AUCTION_BAZAAR_VARIOUS("§bAuction House + Bazaar Navigation Utils"),
        PROJECTS("§bProject Contributions"), //
        BANK_UTILS("§bBank Utilities"), // (Abbvs)
        MAYOR_PERKS("§bMayor Perk Count"), // §b(For Mayor Jerry specifically, it'll show which mayor's perks are active.)
        ;

        final String str;
        PlayerAdvanced(String str) { this.str = str; }
        @Override public String toString() { return str; }
    }
}
