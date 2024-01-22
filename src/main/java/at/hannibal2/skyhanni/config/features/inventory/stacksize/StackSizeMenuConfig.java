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
        name = "Tryhard",
        desc = stackSizeConfigDesc
    )
    @ConfigEditorDraggableList
    public List<PlayerTryhard> playerTryhard = new ArrayList<>(Arrays.asList(
        PlayerTryhard.MENU_NAVIGATION,
        PlayerTryhard.ACCESSORY_BAG_UTILS,
        PlayerTryhard.EVENT_COUNTDOWN_ABBV
    ));

    public enum PlayerTryhard {
        MENU_NAVIGATION("§bMenu Pagination, Sort/Filter Abbvs, Selected Tab"), // §b(Menu Pagination: Shows page numbers.\nSort/Filter Abbvs: AH/Abiphones have their own abbreviation config options.\nSelected Tabs: §a⬇§bs in Community Shop, §a➡§bs in AH + BZ)
        RNG_METER_ODDS("§bRNG Meter Drop Odds"), // (Abbvs)
        COMMUNITY_ESSENCE_UPGRADES("§bCommunity + Essence Shops Upgrade Tiers"), // (#)
        BOOKSHELF_POWER("§bBookshelf Power"), // (#)
        FAME_RANK_BITS("§bFame Rank, Fame Count, Bits Available"), // (Abbvs)
        BOOSTER_COOKIE_POTION_EFFECTS("§bBooster Cookie Duration + Active Potion Effects"), //§b[highest unit of time only: Xy ➡ Xd ➡ Xh ➡ etc...]
        DELIVERIES("§bJerry Deliveries"), // (#s)
        ACCESSORY_BAG_UTILS("§bAccessory Bag Utils"), // (Various)
        EVENT_COUNTDOWN_ABBV("§bEvents \"Start(ing) in\" Countdowns"), //§b[highest unit of time only: Xy ➡ Xd ➡ Xh ➡ etc...]
        SKYBLOCK_ACHIEVEMENT_POINTS("§bSkyBlock Achievements Points"), // (%)
        ;

        final String str;
        PlayerTryhard(String str) { this.str = str; }
        @Override public String toString() { return str; }
    }
}
