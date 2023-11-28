package at.hannibal2.skyhanni.config.features.misc.trevortracker;

import at.hannibal2.skyhanni.config.HasLegacyId;

public enum TrackerEntry implements HasLegacyId {
    TITLE("§b§lTrevor Data Tracker", 0),
    QUESTS_STARTED("§b1,428 §9Quests Started", 1),
    TOTAL_PELTS("§b11,281 §5Total Pelts Gained", 2),
    PELTS_PER_HOUR("§b2,448 §5Pelts Per Hour", 3),
    SPACER_1("", 4),
    KILLED("§b850 §cKilled Animals", 5),
    SELF_KILLING("§b153 §cSelf Killing Animals", 6),
    TRACKABLE("§b788 §fTrackable Animals", 7),
    UNTRACKABLE("§b239 §aUntrackable Animals", 8),
    UNDETECTED("§b115 §9Undetected Animals", 9),
    ENDANGERED("§b73 §5Endangered Animals", 10),
    ELUSIVE("§b12 §6Elusive Animals", 11),
    ;

    private final String str;
    private final int legacyId;

    TrackerEntry(String str, int legacyId) {
        this.str = str;
        this.legacyId = legacyId;
    }

    // Constructor if new enum elements are added post-migration
    TrackerEntry(String str) {
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
