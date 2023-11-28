package at.hannibal2.skyhanni.config.features.garden.cropmilestones;

import at.hannibal2.skyhanni.config.HasLegacyId;

public enum TimeFormatEntry implements HasLegacyId {
    YEAR("Year", 0),
    DAY("Day", 1),
    HOUR("Hour", 2),
    MINUTE("Minute", 3),
    SECOND("Second", 4),
    ;

    private final String str;
    private final int legacyId;

    TimeFormatEntry(String str, int legacyId) {
        this.str = str;
        this.legacyId = legacyId;
    }

    // Constructor if new enum elements are added post-migration
    TimeFormatEntry(String str) {
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
