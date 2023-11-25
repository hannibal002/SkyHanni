package at.hannibal2.skyhanni.features.combat.damageindicator

enum class BossType(
    val fullName: String,
    val bossTypeToggle: Int,
    val shortName: String = fullName,
    val showDeathTime: Boolean = false
) {
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

    SLAYER_ZOMBIE_1("§aRevenant Horror 1", 5, "§aRev 1", showDeathTime = true),
    SLAYER_ZOMBIE_2("§eRevenant Horror 2", 5, "§eRev 2", showDeathTime = true),
    SLAYER_ZOMBIE_3("§cRevenant Horror 3", 5, "§cRev 3", showDeathTime = true),
    SLAYER_ZOMBIE_4("§4Revenant Horror 4", 5, "§4Rev 4", showDeathTime = true),
    SLAYER_ZOMBIE_5("§5Revenant Horror 5", 5, "§5Rev 5", showDeathTime = true),

    SLAYER_SPIDER_1("§aTarantula Broodfather 1", 6, "§aTara 1", showDeathTime = true),
    SLAYER_SPIDER_2("§eTarantula Broodfather 2", 6, "§eTara 2", showDeathTime = true),
    SLAYER_SPIDER_3("§cTarantula Broodfather 3", 6, "§cTara 3", showDeathTime = true),
    SLAYER_SPIDER_4("§4Tarantula Broodfather 4", 6, "§4Tara 4", showDeathTime = true),

    SLAYER_WOLF_1("§aSven Packmaster 1", 7, "§aSven 1", showDeathTime = true),
    SLAYER_WOLF_2("§eSven Packmaster 2", 7, "§eSven 2", showDeathTime = true),
    SLAYER_WOLF_3("§cSven Packmaster 3", 7, "§cSven 3", showDeathTime = true),
    SLAYER_WOLF_4("§4Sven Packmaster 4", 7, "§4Sven 4", showDeathTime = true),

    SLAYER_ENDERMAN_1("§aVoidgloom Seraph 1", 8, "§aVoid 1", showDeathTime = true),
    SLAYER_ENDERMAN_2("§eVoidgloom Seraph 2", 8, "§eVoid 2", showDeathTime = true),
    SLAYER_ENDERMAN_3("§cVoidgloom Seraph 3", 8, "§cVoid 3", showDeathTime = true),
    SLAYER_ENDERMAN_4("§4Voidgloom Seraph 4", 8, "§4Void 4", showDeathTime = true),

    SLAYER_BLAZE_1("§aInferno Demonlord 1", 9, "§aInferno 1", showDeathTime = true),
    SLAYER_BLAZE_2("§aInferno Demonlord 2", 9, "§aInferno 2", showDeathTime = true),
    SLAYER_BLAZE_3("§aInferno Demonlord 3", 9, "§aInferno 3", showDeathTime = true),
    SLAYER_BLAZE_4("§aInferno Demonlord 4", 9, "§aInferno 4", showDeathTime = true),

    SLAYER_BLAZE_TYPHOEUS_1("§aInferno Typhoeus 1", 9, "§aTyphoeus 1"),
    SLAYER_BLAZE_TYPHOEUS_2("§eInferno Typhoeus 2", 9, "§eTyphoeus 2"),
    SLAYER_BLAZE_TYPHOEUS_3("§cInferno Typhoeus 3", 9, "§cTyphoeus 3"),
    SLAYER_BLAZE_TYPHOEUS_4("§cInferno Typhoeus 4", 9, "§cTyphoeus 4"),

    SLAYER_BLAZE_QUAZII_1("§aInferno Quazii 1", 9, "§aQuazii 1"),
    SLAYER_BLAZE_QUAZII_2("§eInferno Quazii 2", 9, "§eQuazii 2"),
    SLAYER_BLAZE_QUAZII_3("§cInferno Quazii 3", 9, "§cQuazii 3"),
    SLAYER_BLAZE_QUAZII_4("§cInferno Quazii 4", 9, "§cQuazii 4"),

    SLAYER_BLOODFIEND_1("§aRiftstalker Bloodfiend 1", 23, "§aBlood 1", showDeathTime = true),
    SLAYER_BLOODFIEND_2("§6Riftstalker Bloodfiend 2", 23, "§6Blood 2", showDeathTime = true),
    SLAYER_BLOODFIEND_3("§cRiftstalker Bloodfiend 3", 23, "§cBlood 3", showDeathTime = true),
    SLAYER_BLOODFIEND_4("§4Riftstalker Bloodfiend 4", 23, "§4Blood 4", showDeathTime = true),
    SLAYER_BLOODFIEND_5("§5Riftstalker Bloodfiend 5", 23, "§5Blood 5", showDeathTime = true),

    HUB_HEADLESS_HORSEMAN("§6Headless Horseman", 10),

    DUNGEON_F1("", 11),
    DUNGEON_F2("", 12),
    DUNGEON_F3("", 13),
    DUNGEON_F4_THORN("§cThorn", 14),
    DUNGEON_F5("§dLivid", 15),
    DUNGEON_F("", 16),
    DUNGEON_75("", 17),

    MINOS_INQUISITOR("§5Minos Inquisitor", 18),
    MINOS_CHAMPION("§2Minos Champion", 18),
    GAIA_CONSTURUCT("§2Gaia Construct", 18),
    MINOTAUR("§2Minotaur", 18),

    THUNDER("§cThunder", 19),
    LORD_JAWBUS("§cLord Jawbus", 19),

    DUMMY("Dummy", 20),
    ARACHNE_SMALL("§cSmall Arachne", 21),
    ARACHNE_BIG("§4Big Arachne", 21),

    // The Rift
    LEECH_SUPREME("§cLeech Supreme", 22),
    BACTE("§aBacte", 22),

    WINTER_REINDRAKE("Reindrake", 24),//TODO fix totally

    GARDEN_PEST_BEETLE("§cBeetle", 25),
    GARDEN_PEST_CRICKET("§cCricket", 25),
    GARDEN_PEST_FLY("§cFly", 25),
    GARDEN_PEST_LOCUST("§cLocust", 25),
    GARDEN_PEST_MITE("§cMite", 25),
    GARDEN_PEST_MOSQUITO("§cMosquito", 25),
    GARDEN_PEST_MOTH("§cMoth", 25),
    GARDEN_PEST_RAT("§cRat", 25),
    GARDEN_PEST_SLUG("§cSlug", 25),
    GARDEN_PEST_EARTHWORM("§cEarthworm", 25),

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
