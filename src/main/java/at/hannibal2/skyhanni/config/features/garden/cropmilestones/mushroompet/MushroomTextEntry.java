package at.hannibal2.skyhanni.config.features.garden.cropmilestones.mushroompet;

import at.hannibal2.skyhanni.config.HasLegacyId;

public enum MushroomTextEntry implements HasLegacyId {
    TITLE("§6Mooshroom Cow Perk", 0),
    MUSHROOM_TIER("§7Mushroom Tier 8", 1),
    NUMBER_OUT_OF_TOTAL("§e6,700§8/§e15,000", 2),
    TIME("§7In §b12m 34s", 3),
    PERCENTAGE("§7Percentage: §e12.34%", 4),
    ;

    private final String str;
    private final int legacyId;

    MushroomTextEntry(String str, int legacyId) {
        this.str = str;
        this.legacyId = legacyId;
    }

    // Constructor if new enum elements are added post-migration
    MushroomTextEntry(String str) {
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
