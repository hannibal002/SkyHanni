package at.hannibal2.skyhanni.config.features.garden.visitor.rewardwarning;

import at.hannibal2.skyhanni.config.HasLegacyId;

public enum ItemWarnEntry implements HasLegacyId {
    FLOWERING_BOUQUET("§9Flowering Bouquet", 0),
    OVERGROWN_GRASS("§9Overgrown Grass", 1),
    GREEN_BANDANA("§9Green Bandana", 2),
    DEDICATION_IV("§9Dedication IV", 3),
    MUSIC_RUNE("§9Music Rune", 4),
    SPACE_HELMET("§cSpace Helmet", 5),
    CULTIVATING_I("§9Cultivating I", 6),
    REPLENISH_I("§9Replenish I", 7),
    ;

    private final String str;
    private final int legacyId;

    ItemWarnEntry(String str, int legacyId) {
        this.str = str;
        this.legacyId = legacyId;
    }

    // Constructor if new enum elements are added post-migration
    ItemWarnEntry(String str) {
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
