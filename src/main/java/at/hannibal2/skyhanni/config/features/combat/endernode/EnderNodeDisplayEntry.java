package at.hannibal2.skyhanni.config.features.combat.endernode;

import at.hannibal2.skyhanni.config.HasLegacyId;

public enum EnderNodeDisplayEntry implements HasLegacyId {
    TITLE("§5§lEnder Node Tracker", 0),
    NODES_MINED("§d1,303 Ender Nodes Mined", 1),
    COINS_MADE("§615.3M Coins Made", 2),
    SPACER_1(" ", 3),
    ENDERMITE_NEST("§b123 §cEndermite Nest", 4),
    ENCHANTED_END_STONE("§b832 §aEnchanted End Stone", 5),
    ENCHANTED_OBSIDIAN("§b230 §aEnchanted Obsidian", 6),
    ENCHANTED_ENDER_PEARL("§b1630 §aEnchanted Ender Pearl", 7),
    GRAND_XP_BOTTLE("§b85 §aGrand Experience Bottle", 8),
    TITANIC_XP_BOTTLE("§b4 §9Titanic Experience Bottle", 9),
    END_STONE_SHULKER("§b15 §9End Stone Shulker", 10),
    END_STONE_GEODE("§b53 §9End Stone Geode", 11),
    MAGICAL_RUNE_I("§b10 §d◆ Magical Rune I", 12),
    ENDER_GAUNTLET("§b24 §5Ender Gauntlet", 13),
    MITE_GEL("§b357 §5Mite Gel", 14),
    SHRIMP_THE_FISH("§b2 §cShrimp The Fish", 15),
    SPACER_2(" ", 16),
    ENDER_ARMOR("§b200 §5Ender Armor", 17),
    ENDER_HELMET("§b24 §5Ender Helmet", 18),
    ENDER_CHESTPLATE("§b24 §5Ender Chestplate", 19),
    ENDER_LEGGINGS("§b24 §5Ender Leggings", 20),
    ENDER_BOOTS("§b24 §5Ender Boots", 21),
    ENDER_NECKLACE("§b24 §5Ender Necklace", 22),
    ENDERMAN_PET("§f10§7-§a8§7-§93§7-§52§7-§61 §fEnderman Pet", 23),
    ;

    private final String str;
    private final int legacyId;

    EnderNodeDisplayEntry(String str, int legacyId) {
        this.str = str;
        this.legacyId = legacyId;
    }

    // Constructor if new enum elements are added post-migration
    EnderNodeDisplayEntry(String str) {
        this(str, -1);
    }

    @Override
    public int getLegacyId() {
        return legacyId;
    }

    @Override
    public String toString() {
        return str;
    }
}
