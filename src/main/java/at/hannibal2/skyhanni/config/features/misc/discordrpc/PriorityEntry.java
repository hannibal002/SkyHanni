package at.hannibal2.skyhanni.config.features.misc.discordrpc;

import at.hannibal2.skyhanni.config.HasLegacyId;

public enum PriorityEntry implements HasLegacyId {
    CROP_MILESTONES("Crop Milestones", 0),
    SLAYER("Slayer", 1),
    STACKING_ENCHANT("Stacking Enchantment", 2),
    DUNGEONS("Dungeon", 3),
    AFK("AFK Indicator", 4),
    ;

    private final String str;
    private final int legacyId;

    PriorityEntry(String str, int legacyId) {
        this.str = str;
        this.legacyId = legacyId;
    }

    // Constructor if new enum elements are added post-migration
    PriorityEntry(String str) {
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
