package at.hannibal2.skyhanni.features.combat.damageindicator

import at.hannibal2.skyhanni.config.features.combat.damageindicator.DamageIndicatorConfig.DamageIndicatorBossEntry

enum class BossType(
    val fullName: String,
    val bossTypeToggle: DamageIndicatorBossEntry,
    val shortName: String = fullName,
    val showDeathTime: Boolean = false
) {
    GENERIC_DUNGEON_BOSS(
        "Generic Dungeon boss",
        DamageIndicatorBossEntry.DUNGEON_ALL
    ),//TODO split into different bosses

    //Nether Mini Bosses
    NETHER_BLADESOUL("§8Bladesoul", DamageIndicatorBossEntry.NETHER_MINI_BOSSES),
    NETHER_MAGMA_BOSS("§4Magma Boss", DamageIndicatorBossEntry.NETHER_MINI_BOSSES),
    NETHER_ASHFANG("§cAshfang", DamageIndicatorBossEntry.NETHER_MINI_BOSSES),
    NETHER_BARBARIAN_DUKE("§eBarbarian Duke", DamageIndicatorBossEntry.NETHER_MINI_BOSSES),
    NETHER_MAGE_OUTLAW("§5Mage Outlaw", DamageIndicatorBossEntry.NETHER_MINI_BOSSES),

    NETHER_VANQUISHER("§5Vanquisher", DamageIndicatorBossEntry.VANQUISHER),

    END_ENDSTONE_PROTECTOR("§cEndstone Protector", DamageIndicatorBossEntry.ENDERSTONE_PROTECTOR),
    END_ENDER_DRAGON("Ender Dragon", DamageIndicatorBossEntry.ENDER_DRAGON),//TODO fix totally

    SLAYER_ZOMBIE_1("§aRevenant Horror 1", DamageIndicatorBossEntry.REVENANT_HORROR, "§aRev 1", showDeathTime = true),
    SLAYER_ZOMBIE_2("§eRevenant Horror 2", DamageIndicatorBossEntry.REVENANT_HORROR, "§eRev 2", showDeathTime = true),
    SLAYER_ZOMBIE_3("§cRevenant Horror 3", DamageIndicatorBossEntry.REVENANT_HORROR, "§cRev 3", showDeathTime = true),
    SLAYER_ZOMBIE_4("§4Revenant Horror 4", DamageIndicatorBossEntry.REVENANT_HORROR, "§4Rev 4", showDeathTime = true),
    SLAYER_ZOMBIE_5("§5Revenant Horror 5", DamageIndicatorBossEntry.REVENANT_HORROR, "§5Rev 5", showDeathTime = true),

    SLAYER_SPIDER_1(
        "§aTarantula Broodfather 1",
        DamageIndicatorBossEntry.TARANTULA_BROODFATHER,
        "§aTara 1",
        showDeathTime = true
    ),
    SLAYER_SPIDER_2(
        "§eTarantula Broodfather 2",
        DamageIndicatorBossEntry.TARANTULA_BROODFATHER,
        "§eTara 2",
        showDeathTime = true
    ),
    SLAYER_SPIDER_3(
        "§cTarantula Broodfather 3",
        DamageIndicatorBossEntry.TARANTULA_BROODFATHER,
        "§cTara 3",
        showDeathTime = true
    ),
    SLAYER_SPIDER_4(
        "§4Tarantula Broodfather 4",
        DamageIndicatorBossEntry.TARANTULA_BROODFATHER,
        "§4Tara 4",
        showDeathTime = true
    ),

    SLAYER_WOLF_1("§aSven Packmaster 1", DamageIndicatorBossEntry.SVEN_PACKMASTER, "§aSven 1", showDeathTime = true),
    SLAYER_WOLF_2("§eSven Packmaster 2", DamageIndicatorBossEntry.SVEN_PACKMASTER, "§eSven 2", showDeathTime = true),
    SLAYER_WOLF_3("§cSven Packmaster 3", DamageIndicatorBossEntry.SVEN_PACKMASTER, "§cSven 3", showDeathTime = true),
    SLAYER_WOLF_4("§4Sven Packmaster 4", DamageIndicatorBossEntry.SVEN_PACKMASTER, "§4Sven 4", showDeathTime = true),

    SLAYER_ENDERMAN_1(
        "§aVoidgloom Seraph 1",
        DamageIndicatorBossEntry.VOIDGLOOM_SERAPH,
        "§aVoid 1",
        showDeathTime = true
    ),
    SLAYER_ENDERMAN_2(
        "§eVoidgloom Seraph 2",
        DamageIndicatorBossEntry.VOIDGLOOM_SERAPH,
        "§eVoid 2",
        showDeathTime = true
    ),
    SLAYER_ENDERMAN_3(
        "§cVoidgloom Seraph 3",
        DamageIndicatorBossEntry.VOIDGLOOM_SERAPH,
        "§cVoid 3",
        showDeathTime = true
    ),
    SLAYER_ENDERMAN_4(
        "§4Voidgloom Seraph 4",
        DamageIndicatorBossEntry.VOIDGLOOM_SERAPH,
        "§4Void 4",
        showDeathTime = true
    ),

    SLAYER_BLAZE_1(
        "§aInferno Demonlord 1",
        DamageIndicatorBossEntry.INFERNO_DEMONLORD,
        "§aInferno 1",
        showDeathTime = true
    ),
    SLAYER_BLAZE_2(
        "§aInferno Demonlord 2",
        DamageIndicatorBossEntry.INFERNO_DEMONLORD,
        "§aInferno 2",
        showDeathTime = true
    ),
    SLAYER_BLAZE_3(
        "§aInferno Demonlord 3",
        DamageIndicatorBossEntry.INFERNO_DEMONLORD,
        "§aInferno 3",
        showDeathTime = true
    ),
    SLAYER_BLAZE_4(
        "§aInferno Demonlord 4",
        DamageIndicatorBossEntry.INFERNO_DEMONLORD,
        "§aInferno 4",
        showDeathTime = true
    ),

    SLAYER_BLAZE_TYPHOEUS_1("§aInferno Typhoeus 1", DamageIndicatorBossEntry.INFERNO_DEMONLORD, "§aTyphoeus 1"),
    SLAYER_BLAZE_TYPHOEUS_2("§eInferno Typhoeus 2", DamageIndicatorBossEntry.INFERNO_DEMONLORD, "§eTyphoeus 2"),
    SLAYER_BLAZE_TYPHOEUS_3("§cInferno Typhoeus 3", DamageIndicatorBossEntry.INFERNO_DEMONLORD, "§cTyphoeus 3"),
    SLAYER_BLAZE_TYPHOEUS_4("§cInferno Typhoeus 4", DamageIndicatorBossEntry.INFERNO_DEMONLORD, "§cTyphoeus 4"),

    SLAYER_BLAZE_QUAZII_1("§aInferno Quazii 1", DamageIndicatorBossEntry.INFERNO_DEMONLORD, "§aQuazii 1"),
    SLAYER_BLAZE_QUAZII_2("§eInferno Quazii 2", DamageIndicatorBossEntry.INFERNO_DEMONLORD, "§eQuazii 2"),
    SLAYER_BLAZE_QUAZII_3("§cInferno Quazii 3", DamageIndicatorBossEntry.INFERNO_DEMONLORD, "§cQuazii 3"),
    SLAYER_BLAZE_QUAZII_4("§cInferno Quazii 4", DamageIndicatorBossEntry.INFERNO_DEMONLORD, "§cQuazii 4"),

    SLAYER_BLOODFIEND_1(
        "§aRiftstalker Bloodfiend 1",
        DamageIndicatorBossEntry.RIFTSTALKER_BLOODFIEND,
        "§aBlood 1",
        showDeathTime = true
    ),
    SLAYER_BLOODFIEND_2(
        "§6Riftstalker Bloodfiend 2",
        DamageIndicatorBossEntry.RIFTSTALKER_BLOODFIEND,
        "§6Blood 2",
        showDeathTime = true
    ),
    SLAYER_BLOODFIEND_3(
        "§cRiftstalker Bloodfiend 3",
        DamageIndicatorBossEntry.RIFTSTALKER_BLOODFIEND,
        "§cBlood 3",
        showDeathTime = true
    ),
    SLAYER_BLOODFIEND_4(
        "§4Riftstalker Bloodfiend 4",
        DamageIndicatorBossEntry.RIFTSTALKER_BLOODFIEND,
        "§4Blood 4",
        showDeathTime = true
    ),
    SLAYER_BLOODFIEND_5(
        "§5Riftstalker Bloodfiend 5",
        DamageIndicatorBossEntry.RIFTSTALKER_BLOODFIEND,
        "§5Blood 5",
        showDeathTime = true
    ),

    HUB_HEADLESS_HORSEMAN("§6Headless Horseman", DamageIndicatorBossEntry.HEADLESS_HORSEMAN),

    DUNGEON_F1("", DamageIndicatorBossEntry.DUNGEON_FLOOR_1),
    DUNGEON_F2("", DamageIndicatorBossEntry.DUNGEON_FLOOR_2),
    DUNGEON_F3("", DamageIndicatorBossEntry.DUNGEON_FLOOR_3),
    DUNGEON_F4_THORN("§cThorn", DamageIndicatorBossEntry.DUNGEON_FLOOR_4),
    DUNGEON_F5("§dLivid", DamageIndicatorBossEntry.DUNGEON_FLOOR_5),
    DUNGEON_F("", DamageIndicatorBossEntry.DUNGEON_FLOOR_6),
    DUNGEON_75("", DamageIndicatorBossEntry.DUNGEON_FLOOR_7),

    MINOS_INQUISITOR("§5Minos Inquisitor", DamageIndicatorBossEntry.DIANA_MOBS),
    MINOS_CHAMPION("§2Minos Champion", DamageIndicatorBossEntry.DIANA_MOBS),
    GAIA_CONSTURUCT("§2Gaia Construct", DamageIndicatorBossEntry.DIANA_MOBS),
    MINOTAUR("§2Minotaur", DamageIndicatorBossEntry.DIANA_MOBS),

    THUNDER("§cThunder", DamageIndicatorBossEntry.SEA_CREATURES),
    LORD_JAWBUS("§cLord Jawbus", DamageIndicatorBossEntry.SEA_CREATURES),

    DUMMY("Dummy", DamageIndicatorBossEntry.DUMMY),
    ARACHNE_SMALL("§cSmall Arachne", DamageIndicatorBossEntry.ARACHNE),
    ARACHNE_BIG("§4Big Arachne", DamageIndicatorBossEntry.ARACHNE),

    // The Rift
    LEECH_SUPREME("§cLeech Supreme", DamageIndicatorBossEntry.THE_RIFT_BOSSES),
    BACTE("§aBacte", DamageIndicatorBossEntry.THE_RIFT_BOSSES),

    WINTER_REINDRAKE("Reindrake", DamageIndicatorBossEntry.REINDRAKE),//TODO fix totally

    GARDEN_PEST_BEETLE("§cBeetle", DamageIndicatorBossEntry.GARDEN_PESTS),
    GARDEN_PEST_CRICKET("§cCricket", DamageIndicatorBossEntry.GARDEN_PESTS),
    GARDEN_PEST_FLY("§cFly", DamageIndicatorBossEntry.GARDEN_PESTS),
    GARDEN_PEST_LOCUST("§cLocust", DamageIndicatorBossEntry.GARDEN_PESTS),
    GARDEN_PEST_MITE("§cMite", DamageIndicatorBossEntry.GARDEN_PESTS),
    GARDEN_PEST_MOSQUITO("§cMosquito", DamageIndicatorBossEntry.GARDEN_PESTS),
    GARDEN_PEST_MOTH("§cMoth", DamageIndicatorBossEntry.GARDEN_PESTS),
    GARDEN_PEST_RAT("§cRat", DamageIndicatorBossEntry.GARDEN_PESTS),
    GARDEN_PEST_SLUG("§cSlug", DamageIndicatorBossEntry.GARDEN_PESTS),
    GARDEN_PEST_EARTHWORM("§cEarthworm", DamageIndicatorBossEntry.GARDEN_PESTS),

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
