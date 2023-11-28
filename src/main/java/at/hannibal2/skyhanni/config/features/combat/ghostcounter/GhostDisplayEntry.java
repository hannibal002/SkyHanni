package at.hannibal2.skyhanni.config.features.combat.ghostcounter;

import at.hannibal2.skyhanni.config.HasLegacyId;

public enum GhostDisplayEntry implements HasLegacyId {
    TITLE("§6Ghosts Counter", 0),
    GHOSTS_KILLED("  §bGhost Killed: 42", 1),
    SORROW("  §bSorrow: 6", 2),
    GHOST_SINCE_SORROW("  §bGhost since Sorrow: 1", 3),
    GHOST_PER_SORROW("  §bGhosts/Sorrow: 5", 4),
    VOLTA("  §bVolta: 6", 5),
    PLASMA("  §bPlasma: 8", 6),
    GHOSTLY_BOOTS("  §bGhostly Boots: 1", 7),
    BAG_OF_CASH("  §bBag Of Cash: 4", 8),
    AVG_MAGIC_FIND("  §bAvg Magic Find: 271", 9),
    SCAVENGER_COINS("  §bScavenger Coins: 15,000", 10),
    KILL_COMBO("  §bKill Combo: 14", 11),
    HIGHEST_KILL_COMBO("  §bHighest Kill Combo: 96", 12),
    SKILL_XP_GAINED("  §bSkill XP Gained: 145,648", 13),
    BESTIARY("  §bBestiary 1: 0/10", 14),
    XP_PER_HOUR("  §bXP/h: 810,410", 15),
    KILLS_PER_HOUR("  §bKills/h: 420", 16),
    ETA("  §bETA: 14d", 17),
    MONEY_PER_HOUR("  §bMoney/h: 13,420,069", 18),
    MONEY_MADE("  §bMoney made: 14B", 19),
    ;

    private final String str;
    private final int legacyId;

    GhostDisplayEntry(String str, int legacyId) {
        this.str = str;
        this.legacyId = legacyId;
    }

    // Constructor if new enum elements are added post-migration
    GhostDisplayEntry(String str) {
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
