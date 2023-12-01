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
        SKYBLOCK_LEVEL("§bSkyblock Level (#, won't work in Rift)"),
        SKILL_GARDEN_DUNGEON_LEVELS("§bSkill + Garden + Dungeoneering Levels (#)"),
        SKILL_AVERAGE("§bSkill Average (#)"),
        COLLECTION_LEVELS_AND_PROGRESS("§bCollection Level + Progress (%)"),
        CRAFTED_MINIONS("§bHighest Crafted Minion Tier + Progress to Next Minion Slot (#)"),
        MUSEUM_PROGRESS("§bMuseum Donation Progress (%, # for Special Items)"),
        PROFILE_ICON("§bSkyblock Profile Type\n§b(Classic/Ironman/Stranded/Bingo)"),
        PET_SCORE_STATUS("§bPet Score (#) + \"None\" Pet Status Indicator (c§l✖§b)"),
        ESSENCE_COUNTS("§bEssence Counts\n§b(# in Rewards Chests, Abbv in Essence Shops)"),
        MINION_QUICK_UPGRADE("§bQuick Upgrade Missing Count (#)");

        final String str;

        PlayerGeneral(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    @Expose
    @ConfigOption(
        name = "Advanced",
        desc = "Showing various pieces of information as a stack size for these menu items.\nSome values may be truncated percentages or §a✔§r§7s.\n§c§lWARNING§r§c: If you have any respect for your FPS, please §l§odon't§r§c enable §l§oeverything§r§c at once."
    )
    @ConfigEditorDraggableList
    public List<PlayerAdvanced> playerAdvanced = new ArrayList<>(Arrays.asList(
        PlayerAdvanced.UNLOCKED_RECIPES,
        PlayerAdvanced.AUCTION_BAZAAR_VARIOUS,
        PlayerAdvanced.DOJO_PROGRESS,
        PlayerAdvanced.BANK_UTILS,
        PlayerAdvanced.MAYOR_PERKS
    ));

    public enum PlayerAdvanced {
        UNLOCKED_RECIPES("§bUnlocked Recipes (%)"),
        FAIRY_ENIGMA_SOULS_QUESTS("§bCollected Fairy/Enigma Souls + Completed Quests (#)"),
        TRADES_UNLOCKED("§bTrades Unlocked (%)"),
        WARDROBE_SLOT("§bWardrobe Slot (#)"),
        ABBV_STATS("§bSkyblock Stat Names (Abbvs)"),
        CUTE_NAME("§bSkyblock Profile Fruits Name (Abbvs)"),
        AUCTION_BAZAAR_VARIOUS("§bAuction House + Bazaar (Various)"),
        DOJO_PROGRESS("§bDojo Progress (Abbv)"),
        BANK_UTILS("§bBank Utilities (Abbvs)"),
        MAYOR_PERKS("§bMayor Perk Count (#)\n§b(For Mayor Jerry specifically, it'll show which mayor's perks are active.)");

        final String str;

        PlayerAdvanced(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    @Expose
    @ConfigOption(
        name = "Tryhard",
        desc = "Showing various pieces of information as a stack size for these menu items.\nSome values may be truncated percentages or §a✔§r§7s.\n§c§lWARNING§r§c: If you have any respect for your FPS, please §l§odon't§r§c enable §l§oeverything§r§c at once."
    )
    @ConfigEditorDraggableList
    public List<PlayerTryhard> playerTryhard = new ArrayList<>(Arrays.asList(
        PlayerTryhard.MENU_NAVIGATION,
        PlayerTryhard.ACCESSORY_BAG_UTILS,
        PlayerTryhard.EVENT_COUNTDOWN_ABBV
    ));

    public enum PlayerTryhard {
        MENU_NAVIGATION("§bMenu Pagination (#) + Sorting/Filtering Abbreviations\n§b(Note: AH/Abiphones have their seperate sorting/filtering abbv configs.)"),
        RNG_METER_ODDS("§bRNG Meter Drop Odds (Abbvs)"),
        COMMUNITY_ESSENCE_UPGRADES("§bCommunity Shop + Essence Shop Upgrade Tiers (#)"),
        SELECTED_TAB("§bSelected Tab\n§b(§a⬇§bs in Community Shop, §a➡§bs in Auction + Bazaar)"),
        FAME_RANK_BITS("§bFame Rank, Abbv'd Fame Count, Bits Available (Abbvs)"),
        BOOSTER_COOKIE_DURATION("§bBooster Cookie Duration (highest unit of time only)\n§b[Xy ➡ Xd ➡ Xh ➡ etc...]"),
        ACTIVE_POTION_COUNT("§bCurrently Active Potion Effects Count (#)"),
        ACCESSORY_BAG_UTILS("§bAccessory Bag Utilities (Various)"),
        EVENT_COUNTDOWN_ABBV("§bEvents \"Start(ing) in\" Countdowns (Abbvs)\n§b[highest unit of time only: Xy ➡ Xd ➡ Xh ➡ etc...]"),
        SKYBLOCK_ACHIEVEMENT_POINTS("§bSkyBlock Achievements Points (%)");

        final String str;

        PlayerTryhard(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

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
        JACOBS_MEDALS("§bFarming Contests Medal Inventory (§6§lG§r§f§lS§r§c§lB§r§b)"),
        VISITORS_LOGBOOK_COUNTDOWN("§bVisitor's Logbook Countdown (#, highest unit of time only)"),
        VISITOR_MILESTONES("§bVisitor Milestones Progress (%)"),
        VISITOR_NPC_RARITIES("§bGarden Visitor's Logbook NPC Rarities (Abbv)"),
        COMPOSTER_INSERT_ABBV("§bComposter \"Insert from \" Counts (Abbv)");

        final String str;

        Farming(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

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
        HOTM_PERK_LEVELS("§bHeart of the Mountain Perk Levels (#, §c#§b when disabled)"),
        HOTM_OVERALL_TIERS("§bHOTM Tiers Progress (%)"),
        CRYSTAL_HOLLOWS_NUCLEUS("§bCrystal Hollows Crystal Progress (§aF§eNP§cNF§b)");

        final String str;

        Mining(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    @Expose
    @ConfigOption(
        name = "§aCombat§7",
        desc = "Showing various pieces of information as a stack size for these menu items.\nSome values may be truncated percentages or §a✔§r§7s.\n§c§lWARNING§r§c: If you have any respect for your FPS, please §l§odon't§r§c enable §l§oeverything§r§c at once."
    )
    @ConfigEditorDraggableList
    public List<Combat> combat = new ArrayList<>(Arrays.asList(
        Combat.BESTIARY_LEVEL,
        Combat.BESTIARY_OVERALL_FAMILY_PROGRESS,
        Combat.SLAYER_LEVELS
    ));

    public enum Combat {
        BESTIARY_LEVEL("§bBestiary Level (#)"),
        BESTIARY_OVERALL_FAMILY_PROGRESS("§bBestiary Progress (%, Overall + per Family)"),
        SLAYER_LEVELS("§bCurrent Slayer Levels (#)"),
        SLAYER_COMBAT_WISDOM_BUFF("§bSlayer Combat Wisdom Buff (#)"),
        RNG_METER_PROGRESS("§bSlayer/Catacombs RNG Meter Progress (%)"),
        UNLOCKED_SLAYER_RECIPES("§bUnlocked Slayer Recipes (#)");

        final String str;

        Combat(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

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
        GUIDE_PROGRESS("§bSkyblock Guide Progress (%)"),
        WAYS_TO_LEVEL_UP_PROGRESS("§bSkyblock Ways To Level Up Tasks (%)"),
        SB_LEVELING_REWARDS("§bSkyblock Leveling Rewards Progress (%)"),
        EMBLEMS_UNLOCKED("§bEmblems Unlocked (#)");

        final String str;

        SBLeveling(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    @Expose
    @ConfigOption(
        name = "§aAb§9ip§5ho§6ne§ds§7",
        desc = "Showing various pieces of information as a stack size for these menu items.\nSome values may be truncated percentages or §a✔§r§7s.\n§c§lWARNING§r§c: If you have any respect for your FPS, please §l§odon't§r§c enable §l§oeverything§r§c at once."
    )
    @ConfigEditorDraggableList
    public List<Abiphone> abiphone = new ArrayList<>(Arrays.asList(
        Abiphone.CONTACTS_DIRECTORY,
        Abiphone.DO_NOT_DISTURB,
        Abiphone.RELAYS_COMPLETED,
        Abiphone.SELECTED_RINGTONE,
        Abiphone.NAVIGATION
    ));

    public enum Abiphone {
        CONTACTS_DIRECTORY("§bContacts Directory (#)"),
        DO_NOT_DISTURB("§bDND Indicator (§c§l✖§b)"),
        RELAYS_COMPLETED("§bRelays Finished (#)"),
        SELECTED_RINGTONE("§bSelected Ringtone (Abbv)"),
        TIC_TAC_TOE("§bTic Tac Toe Stats (§aW§eT§cL§b)"),
        SNAKE("§bSnake Highest Score (#)"),
        NAVIGATION("§bSorting/Filtering Abbreviations");

        final String str;

        Abiphone(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    @Expose
    @ConfigOption(
        name = "§aBingo Card§7",
        desc = "Showing various pieces of information as a stack size for these menu items.\nSome values may be truncated percentages or §a✔§r§7s.\n§c§lWARNING§r§c: If you have any respect for your FPS, please §l§odon't§r§c enable §l§oeverything§r§c at once."
    )
    @ConfigEditorDraggableList
    public List<Bingo> bingo = new ArrayList<>(Arrays.asList(
        Bingo.SECRET_BINGO_DISCOVERY
    ));

    public enum Bingo {
        SECRET_BINGO_DISCOVERY("§bSecret Bingo Goal Discovery (#, caps at 10k)"),
        ROW_COLUMN_DIAGONAL_PROGRESS("§bRow/Diagonal/Column Progress"),
        ;

        final String str;

        Bingo(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }
}
