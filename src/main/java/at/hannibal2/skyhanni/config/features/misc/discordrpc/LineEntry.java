package at.hannibal2.skyhanni.config.features.misc.discordrpc;

import at.hannibal2.skyhanni.config.HasLegacyId;

public enum LineEntry implements HasLegacyId {
    NOTHING("Nothing", 0),
    LOCATION("Location", 1),
    PURSE("Purse", 2),
    BITS("Bits", 3),
    STATS("Stats", 4),
    HELD_ITEM("Held Item", 5),
    SKYBLOCK_DATE("SkyBlock Date", 6),
    PROFILE("Profile", 7),
    SLAYER("Slayer", 8),
    CUSTOM("Custom", 9),
    DYNAMIC("Dynamic", 10),
    CROP_MILESTONE("Crop Milestone", 11),
    CURRENT_PET("Current Pet", 12),
    ;

    private final String str;
    private final int legacyId;

    LineEntry(String str, int legacyId) {
        this.str = str;
        this.legacyId = legacyId;
    }

    // Constructor if new enum elements are added post-migration
    LineEntry(String str) {
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
