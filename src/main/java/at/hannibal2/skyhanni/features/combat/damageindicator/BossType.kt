package at.hannibal2.skyhanni.features.combat.damageindicator

import at.hannibal2.skyhanni.config.features.combat.damageindicator.DamageIndicatorConfig.DamageIndicatorBosses

enum class BossType(
    val fullName: String,
    val bossTypeToggle: DamageIndicatorBosses,
    val shortName: String = fullName,
    val showDeathTime: Boolean = false
) {
    GENERIC_DUNGEON_BOSS("Generic Dungeon boss", DamageIndicatorBosses.DUNGEON_ALL),//TODO split into different bosses

    //Nether Mini Bosses
    NETHER_BLADESOUL("§8Bladesoul", DamageIndicatorBosses.NETHER_MINI_BOSSES),
    NETHER_MAGMA_BOSS("§4Magma Boss", DamageIndicatorBosses.NETHER_MINI_BOSSES),
    NETHER_ASHFANG("§cAshfang", DamageIndicatorBosses.NETHER_MINI_BOSSES),
    NETHER_BARBARIAN_DUKE("§eBarbarian Duke", DamageIndicatorBosses.NETHER_MINI_BOSSES),
    NETHER_MAGE_OUTLAW("§5Mage Outlaw", DamageIndicatorBosses.NETHER_MINI_BOSSES),

    NETHER_VANQUISHER("§5Vanquisher", DamageIndicatorBosses.VANQUISHER),

    END_ENDSTONE_PROTECTOR("§cEndstone Protector", DamageIndicatorBosses.ENDERSTONE_PROTECTOR),
    END_ENDER_DRAGON("Ender Dragon", DamageIndicatorBosses.ENDER_DRAGON),//TODO fix totally

    SLAYER_ZOMBIE_1("§aRevenant Horror 1", DamageIndicatorBosses.REVENANT_HORROR, "§aRev 1", showDeathTime = true),
    SLAYER_ZOMBIE_2("§eRevenant Horror 2", DamageIndicatorBosses.REVENANT_HORROR, "§eRev 2", showDeathTime = true),
    SLAYER_ZOMBIE_3("§cRevenant Horror 3", DamageIndicatorBosses.REVENANT_HORROR, "§cRev 3", showDeathTime = true),
    SLAYER_ZOMBIE_4("§4Revenant Horror 4", DamageIndicatorBosses.REVENANT_HORROR, "§4Rev 4", showDeathTime = true),
    SLAYER_ZOMBIE_5("§5Revenant Horror 5", DamageIndicatorBosses.REVENANT_HORROR, "§5Rev 5", showDeathTime = true),

    SLAYER_SPIDER_1(
        "§aTarantula Broodfather 1",
        DamageIndicatorBosses.TARANTULA_BROODFATHER,
        "§aTara 1",
        showDeathTime = true
    ),
    SLAYER_SPIDER_2(
        "§eTarantula Broodfather 2",
        DamageIndicatorBosses.TARANTULA_BROODFATHER,
        "§eTara 2",
        showDeathTime = true
    ),
    SLAYER_SPIDER_3(
        "§cTarantula Broodfather 3",
        DamageIndicatorBosses.TARANTULA_BROODFATHER,
        "§cTara 3",
        showDeathTime = true
    ),
    SLAYER_SPIDER_4(
        "§4Tarantula Broodfather 4",
        DamageIndicatorBosses.TARANTULA_BROODFATHER,
        "§4Tara 4",
        showDeathTime = true
    ),

    SLAYER_WOLF_1("§aSven Packmaster 1", DamageIndicatorBosses.SVEN_PACKMASTER, "§aSven 1", showDeathTime = true),
    SLAYER_WOLF_2("§eSven Packmaster 2", DamageIndicatorBosses.SVEN_PACKMASTER, "§eSven 2", showDeathTime = true),
    SLAYER_WOLF_3("§cSven Packmaster 3", DamageIndicatorBosses.SVEN_PACKMASTER, "§cSven 3", showDeathTime = true),
    SLAYER_WOLF_4("§4Sven Packmaster 4", DamageIndicatorBosses.SVEN_PACKMASTER, "§4Sven 4", showDeathTime = true),

    SLAYER_ENDERMAN_1("§aVoidgloom Seraph 1", DamageIndicatorBosses.VOIDGLOOM_SERAPH, "§aVoid 1", showDeathTime = true),
    SLAYER_ENDERMAN_2("§eVoidgloom Seraph 2", DamageIndicatorBosses.VOIDGLOOM_SERAPH, "§eVoid 2", showDeathTime = true),
    SLAYER_ENDERMAN_3("§cVoidgloom Seraph 3", DamageIndicatorBosses.VOIDGLOOM_SERAPH, "§cVoid 3", showDeathTime = true),
    SLAYER_ENDERMAN_4("§4Voidgloom Seraph 4", DamageIndicatorBosses.VOIDGLOOM_SERAPH, "§4Void 4", showDeathTime = true),

    SLAYER_BLAZE_1(
        "§aInferno Demonlord 1",
        DamageIndicatorBosses.INFERNO_DEMONLORD,
        "§aInferno 1",
        showDeathTime = true
    ),
    SLAYER_BLAZE_2(
        "§aInferno Demonlord 2",
        DamageIndicatorBosses.INFERNO_DEMONLORD,
        "§aInferno 2",
        showDeathTime = true
    ),
    SLAYER_BLAZE_3(
        "§aInferno Demonlord 3",
        DamageIndicatorBosses.INFERNO_DEMONLORD,
        "§aInferno 3",
        showDeathTime = true
    ),
    SLAYER_BLAZE_4(
        "§aInferno Demonlord 4",
        DamageIndicatorBosses.INFERNO_DEMONLORD,
        "§aInferno 4",
        showDeathTime = true
    ),

    SLAYER_BLAZE_TYPHOEUS_1("§aInferno Typhoeus 1", DamageIndicatorBosses.INFERNO_DEMONLORD, "§aTyphoeus 1"),
    SLAYER_BLAZE_TYPHOEUS_2("§eInferno Typhoeus 2", DamageIndicatorBosses.INFERNO_DEMONLORD, "§eTyphoeus 2"),
    SLAYER_BLAZE_TYPHOEUS_3("§cInferno Typhoeus 3", DamageIndicatorBosses.INFERNO_DEMONLORD, "§cTyphoeus 3"),
    SLAYER_BLAZE_TYPHOEUS_4("§cInferno Typhoeus 4", DamageIndicatorBosses.INFERNO_DEMONLORD, "§cTyphoeus 4"),

    SLAYER_BLAZE_QUAZII_1("§aInferno Quazii 1", DamageIndicatorBosses.INFERNO_DEMONLORD, "§aQuazii 1"),
    SLAYER_BLAZE_QUAZII_2("§eInferno Quazii 2", DamageIndicatorBosses.INFERNO_DEMONLORD, "§eQuazii 2"),
    SLAYER_BLAZE_QUAZII_3("§cInferno Quazii 3", DamageIndicatorBosses.INFERNO_DEMONLORD, "§cQuazii 3"),
    SLAYER_BLAZE_QUAZII_4("§cInferno Quazii 4", DamageIndicatorBosses.INFERNO_DEMONLORD, "§cQuazii 4"),

    SLAYER_BLOODFIEND_1(
        "§aRiftstalker Bloodfiend 1",
        DamageIndicatorBosses.RIFTSTALKER_BLOODFIEND,
        "§aBlood 1",
        showDeathTime = true
    ),
    SLAYER_BLOODFIEND_2(
        "§6Riftstalker Bloodfiend 2",
        DamageIndicatorBosses.RIFTSTALKER_BLOODFIEND,
        "§6Blood 2",
        showDeathTime = true
    ),
    SLAYER_BLOODFIEND_3(
        "§cRiftstalker Bloodfiend 3",
        DamageIndicatorBosses.RIFTSTALKER_BLOODFIEND,
        "§cBlood 3",
        showDeathTime = true
    ),
    SLAYER_BLOODFIEND_4(
        "§4Riftstalker Bloodfiend 4",
        DamageIndicatorBosses.RIFTSTALKER_BLOODFIEND,
        "§4Blood 4",
        showDeathTime = true
    ),
    SLAYER_BLOODFIEND_5(
        "§5Riftstalker Bloodfiend 5",
        DamageIndicatorBosses.RIFTSTALKER_BLOODFIEND,
        "§5Blood 5",
        showDeathTime = true
    ),

    HUB_HEADLESS_HORSEMAN("§6Headless Horseman", DamageIndicatorBosses.HEADLESS_HORSEMAN),

    DUNGEON_F1("", DamageIndicatorBosses.DUNGEON_FLOOR_1),
    DUNGEON_F2("", DamageIndicatorBosses.DUNGEON_FLOOR_2),
    DUNGEON_F3("", DamageIndicatorBosses.DUNGEON_FLOOR_3),
    DUNGEON_F4_THORN("§cThorn", DamageIndicatorBosses.DUNGEON_FLOOR_4),
    DUNGEON_F5("§dLivid", DamageIndicatorBosses.DUNGEON_FLOOR_5),
    DUNGEON_F("", DamageIndicatorBosses.DUNGEON_FLOOR_6),
    DUNGEON_75("", DamageIndicatorBosses.DUNGEON_FLOOR_7),

    MINOS_INQUISITOR("§5Minos Inquisitor", DamageIndicatorBosses.DIANA_MOBS),
    MINOS_CHAMPION("§2Minos Champion", DamageIndicatorBosses.DIANA_MOBS),
    GAIA_CONSTURUCT("§2Gaia Construct", DamageIndicatorBosses.DIANA_MOBS),
    MINOTAUR("§2Minotaur", DamageIndicatorBosses.DIANA_MOBS),

    THUNDER("§cThunder", DamageIndicatorBosses.SEA_CREATURES),
    LORD_JAWBUS("§cLord Jawbus", DamageIndicatorBosses.SEA_CREATURES),

    DUMMY("Dummy", DamageIndicatorBosses.DUMMY),
    ARACHNE_SMALL("§cSmall Arachne", DamageIndicatorBosses.ARACHNE),
    ARACHNE_BIG("§4Big Arachne", DamageIndicatorBosses.ARACHNE),

    // The Rift
    LEECH_SUPREME("§cLeech Supreme", DamageIndicatorBosses.THE_RIFT_BOSSES),
    BACTE("§aBacte", DamageIndicatorBosses.THE_RIFT_BOSSES),

    WINTER_REINDRAKE("Reindrake", DamageIndicatorBosses.REINDRAKE),//TODO fix totally

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
