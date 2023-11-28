package at.hannibal2.skyhanni.config.features.combat.damageindicator;

import at.hannibal2.skyhanni.config.HasLegacyId;

public enum DamageIndicatorBossEntry implements HasLegacyId {
    DUNGEON_ALL("§bDungeon All", 0),
    NETHER_MINI_BOSSES("§bNether Mini Bosses", 1),
    VANQUISHER("§bVanquisher", 2),
    ENDERSTONE_PROTECTOR("§bEndstone Protector (not tested)", 3),
    ENDER_DRAGON("§bEnder Dragon (not finished)", 4),
    REVENANT_HORROR("§bRevenant Horror", 5),
    TARANTULA_BROODFATHER("§bTarantula Broodfather", 6),
    SVEN_PACKMASTER("§bSven Packmaster", 7),
    VOIDGLOOM_SERAPH("§bVoidgloom Seraph", 8),
    INFERNO_DEMONLORD("§bInferno Demonlord", 9),
    HEADLESS_HORSEMAN("§bHeadless Horseman (bugged)", 10),
    DUNGEON_FLOOR_1("§bDungeon Floor 1", 11),
    DUNGEON_FLOOR_2("§bDungeon Floor 2", 12),
    DUNGEON_FLOOR_3("§bDungeon Floor 3", 13),
    DUNGEON_FLOOR_4("§bDungeon Floor 4", 14),
    DUNGEON_FLOOR_5("§bDungeon Floor 5", 15),
    DUNGEON_FLOOR_6("§bDungeon Floor 6", 16),
    DUNGEON_FLOOR_7("§bDungeon Floor 7", 17),
    DIANA_MOBS("§bDiana Mobs", 18),
    SEA_CREATURES("§bSea Creatures", 19),
    DUMMY("Dummy", 20),
    ARACHNE("§bArachne", 21),
    THE_RIFT_BOSSES("§bThe Rift Bosses", 22),
    RIFTSTALKER_BLOODFIEND("§bRiftstalker Bloodfiend", 23),
    REINDRAKE("§6Reindrake", 24),
    GARDEN_PESTS("§aGarden Pests", 25),
    ;

    private final String str;
    private final int legacyId;

    DamageIndicatorBossEntry(String str, int legacyId) {
        this.str = str;
        this.legacyId = legacyId;
    }

    // Constructor if new enum elements are added post-migration
    DamageIndicatorBossEntry(String str) {
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
