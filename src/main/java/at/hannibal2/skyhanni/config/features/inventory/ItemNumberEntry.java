package at.hannibal2.skyhanni.config.features.inventory;

import at.hannibal2.skyhanni.config.HasLegacyId;

public enum ItemNumberEntry implements HasLegacyId {
    MASTER_STAR_TIER("§bMaster Star Tier", 0),
    MASTER_SKULL_TIER("§bMaster Skull Tier", 1),
    DUNGEON_HEAD_FLOOR_NUMBER("§bDungeon Head Floor Number", 2),
    NEW_YEAR_CAKE("§bNew Year Cake", 3),
    PET_LEVEL("§bPet Level", 4),
    MINION_TIER("§bMinion Tier", 5),
    CRIMSON_ARMOR("§bCrimson Armor", 6),
    REMOVED("§7(Removed)", 7),
    KUUDRA_KEY("§bKuudra Key", 8),
    SKILL_LEVEL("§bSkill Level", 9),
    COLLECTION_LEVEL("§bCollection Level", 10),
    RANCHERS_BOOTS_SPEED("§bRancher's Boots speed", 11),
    LARVA_HOOK("§bLarva Hook", 12),
    DUNGEON_POTION_LEVEL("§bDungeon Potion Level", 13),
    VACUUM_GARDEN("§bVacuum (Garden)", 14),
    ;

    private final String str;
    private final int legacyId;

    ItemNumberEntry(String str, int legacyId) {
        this.str = str;
        this.legacyId = legacyId;
    }

    // Constructor if new enum elements are added post-migration
    ItemNumberEntry(String str) {
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
