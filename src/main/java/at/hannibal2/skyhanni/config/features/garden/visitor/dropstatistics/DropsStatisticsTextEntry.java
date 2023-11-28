package at.hannibal2.skyhanni.config.features.garden.visitor.dropstatistics;

import at.hannibal2.skyhanni.config.HasLegacyId;

public enum DropsStatisticsTextEntry implements HasLegacyId {
    TITLE("§e§lVisitor Statistics", 0),
    TOTAL_VISITORS("§e1,636 Total", 1),
    VISITORS_BY_RARITY("§a1,172§f-§9382§f-§681§f-§d2§f-§c1", 2),
    ACCEPTED("§21,382 Accepted", 3),
    DENIED("§c254 Denied", 4),
    SPACER_1(" ", 5),
    COPPER("§c62,072 Copper", 6),
    FARMING_EXP("§33.2m Farming EXP", 7),
    COINS_SPENT("§647.2m Coins Spent", 8),
    FLOWERING_BOUQUET("§b23 §9Flowering Bouquet", 9),
    OVERGROWN_GRASS("§b4 §9Overgrown Grass", 10),
    GREEN_BANDANA("§b2 §5Green Bandana", 11),
    DEDICATION_IV("§b1 §9Dedication IV", 12),
    MUSIC_RUNE_I("§b6 §b◆ Music Rune I", 13),
    SPACE_HELMET("§b1 §cSpace Helmet", 14),
    CULTIVATING_I("§b1 §9Cultivating I", 15),
    REPLENISH_I("§b1 §9Replenish I", 16),
    SPACER_2(" ", 17),
    GARDEN_EXP("§212,600 Garden EXP", 18),
    BITS("§b4.2k Bits", 19),
    MITHRIL_POWDER("§220k Mithril Powder", 20),
    GEMSTONE_POWDER("§d18k Gemstone Powder", 21),
    ;

    private final String str;
    private final int legacyId;

    DropsStatisticsTextEntry(String str, int legacyId) {
        this.str = str;
        this.legacyId = legacyId;
    }

    // Constructor if new enum elements are added post-migration
    DropsStatisticsTextEntry(String str) {
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
