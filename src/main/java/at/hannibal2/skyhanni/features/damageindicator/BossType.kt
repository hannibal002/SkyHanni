package at.hannibal2.skyhanni.features.damageindicator

enum class BossType(val fullName: String, val bossTypeToggle: Int, val shortName: String = fullName) {
    GENERIC_DUNGEON_BOSS("Generic Dungeon boss", 0),//TODO split into different bosses

    //Nether Mini Bosses
    NETHER_BLADESOUL("§8Bladesoul", 1),
    NETHER_MAGMA_BOSS("§4Magma Boss", 1),
    NETHER_ASHFANG("§cAshfang", 1),
    NETHER_BARBARIAN_DUKE("§eBarbarian Duke", 1),
    NETHER_MAGE_OUTLAW("§5Mage Outlaw", 1),

    NETHER_VANQUISHER("§5Vanquisher", 2),

    END_ENDSTONE_PROTECTOR("§cEndstone Protector", 3),
    END_ENDER_DRAGON("Ender Dragon", 4),//TODO fix totally

    SLAYER_ZOMBIE_1("§aRevenant Horror 1", 5, "§aRev 1"),
    SLAYER_ZOMBIE_2("§eRevenant Horror 2", 5, "§eRev 2"),
    SLAYER_ZOMBIE_3("§cRevenant Horror 3", 5, "§cRev 3"),
    SLAYER_ZOMBIE_4("§4Revenant Horror 4", 5, "§4Rev 4"),
    SLAYER_ZOMBIE_5("§5Revenant Horror 5", 5, "§5Rev 5"),

    SLAYER_SPIDER_1("§aTarantula Broodfather 1", 6, "§aTara 1"),
    SLAYER_SPIDER_2("§eTarantula Broodfather 2", 6, "§eTara 2"),
    SLAYER_SPIDER_3("§cTarantula Broodfather 3", 6, "§cTara 3"),
    SLAYER_SPIDER_4("§4Tarantula Broodfather 4", 6, "§4Tara 4"),

    SLAYER_WOLF_1("§aSven Packmaster 1", 7, "§aSven 1"),
    SLAYER_WOLF_2("§eSven Packmaster 2", 7, "§eSven 2"),
    SLAYER_WOLF_3("§cSven Packmaster 3", 7, "§cSven 3"),
    SLAYER_WOLF_4("§4Sven Packmaster 4", 7, "§4Sven 4"),

    SLAYER_ENDERMAN_1("§aVoidgloom Seraph 1", 8, "§aVoid 1"),
    SLAYER_ENDERMAN_2("§eVoidgloom Seraph 2", 8, "§eVoid 2"),
    SLAYER_ENDERMAN_3("§cVoidgloom Seraph 3", 8, "§cVoid 3"),
    SLAYER_ENDERMAN_4("§4Voidgloom Seraph 4", 8, "§4Void 4"),

    SLAYER_BLAZE_1("§aInferno Demonlord 1", 9, "§aInferno 1"),

    HUB_HEADLESS_HORSEMAN("§6Headless Horseman", 10),

    DUNGEON_F1("", 11),
    DUNGEON_F2("", 12),
    DUNGEON_F3("", 13),
    DUNGEON_F4_THORN("§cThorn", 14),
    DUNGEON_F5("", 15),
    DUNGEON_F("", 16),
    DUNGEON_75("", 17),

    DUMMY("Dummy", 18),

    //TODO arachne

    //TODO corelone
    //TODO bal


    /**
     * TODO dungeon mini bosses
     * shadow assassin
     * lost adventurer
     * frozen adventurer
     * king midas
     * in blood room: bonzo, scarf, ??
     * f7 blood room giants
     *
     */

    //TODO diana mythological creatures
}