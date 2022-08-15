package at.hannibal2.skyhanni.damageindicator

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

    SPIDER_SLAYER("Spider Slayer", 6, "Spider"),
    WOLF_SLAYER("Wolf Slayer", 7, "Wolf"),

    SLAYER_ENDERMAN_1("§aVoidgloom Seraph 1", 8),
    SLAYER_ENDERMAN_2("§eVoidgloom Seraph 2", 8),
    SLAYER_ENDERMAN_3("§cVoidgloom Seraph 3", 8),
    SLAYER_ENDERMAN_4("§4Voidgloom Seraph 4", 8),

    SLAYER_BLAZE_1("§aInferno Demonlord 1", 9),

    HUB_HEADLESS_HORSEMAN("§6Headless Horseman", 10),

    DUNGEON_F1("", 11),
    DUNGEON_F2("", 12),
    DUNGEON_F3("", 13),
    DUNGEON_F4_THORN("§cThorn", 14),
    DUNGEON_F5("", 15),
    DUNGEON_F("", 16),
    DUNGEON_75("", 17),

    //TODO arachne

    //TODO corelone
    //TODO bal


    /**
     * TODO dungeon mini bosses
     * shadow assassin
     * lost adventurer
     * frozen adventurer
     * king midas
     * silverfish 2b one tap - deathmite outside trap
     * in blood room: bonzo, scarf, ??
     * f7 blood room giants
     *
     */

    //TODO diana mythological creatures
}