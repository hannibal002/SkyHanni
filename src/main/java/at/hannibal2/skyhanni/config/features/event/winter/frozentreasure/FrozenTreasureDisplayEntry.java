package at.hannibal2.skyhanni.config.features.event.winter.frozentreasure;

import at.hannibal2.skyhanni.config.HasLegacyId;

public enum FrozenTreasureDisplayEntry implements HasLegacyId {
    TITLE("§1§lFrozen Treasure Tracker", 0),
    TREASURES_MINED("§61,636 Treasures Mined", 1),
    TOTAL_ICE("§33.2m Total Ice", 2),
    ICE_PER_HOUR("§3342,192 Ice/hr", 3),
    COMPACT_PROCS("§81,002 Compact Procs", 4),
    SPACER_1(" ", 5),
    WHITE_GIFT("§b182 §fWhite Gift", 6),
    GREEN_GIFT("§b94 §aGreen Gift", 7),
    RED_GIFT("§b17 §9§cRed Gift", 8),
    PACKED_ICE("§b328 §fPacked Ice", 9),
    ENCHANTED_ICE("§b80 §aEnchanted Ice", 10),
    ENCHANTED_PACKED_ICE("§b4 §9Enchanted Packed Ice", 11),
    ICE_BAIT("§b182 §aIce Bait", 12),
    GLOWY_CHUM_BAIT("§b3 §aGlowy Chum Bait", 13),
    GLACIAL_FRAGMENT("§b36 §5Glacial Fragment", 14),
    GLACIAL_TALISMAN("§b6 §fGlacial Talisman", 15),
    SPACER_2(" ", 16);

    private final String str;
    private final int legacyId;

    FrozenTreasureDisplayEntry(String str, int legacyId) {
        this.str = str;
        this.legacyId = legacyId;
    }

    // Constructor if new enum elements are added post-migration
    FrozenTreasureDisplayEntry(String str) {
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
