package at.hannibal2.skyhanni.damageindicator

enum class BossType(val fullName: String, val bossTypeToggle: Int, val shortName: String = fullName) {
    DUNGEON("Generic Dungeon boss", 0),//TODO split into different bosses

    //Nether Mini Bosses
    NETHER_BLADESOUL("§8Bladesoul", 1),
    NETHER_MAGMA_BOSS("§4Magma Boss", 1),
    NETHER_ASHFANG("§cAshfang", 1),
    NETHER_BARBARIAN_DUKE("§eBarbarian Duke", 1),
    NETHER_MAGE_OUTLAW("§5Mage Outlaw", 1),

    NETHER_VANQUISHER("§5Vanquisher", 2),

    END_ENDSTONE_PROTECTOR("§cEndstone Protector", 3),
    END_ENDER_DRAGON("Ender Dragon", 4),//TODO fix totally

    //TODO more seperated slayer variants
    HUB_REVENANT_HORROR("§5Revenant Horror 5", 5, "§5Rev 5"),
    SPIDER_SLAYER("Spider Slayer", 6, "Spider"),
    WOLF_SLAYER("Wolf Slayer", 7, "Wolf"),

    SLAYER_ENDERMAN_1("§aVoidgloom Seraph 1", 8),
    SLAYER_ENDERMAN_2("§eVoidgloom Seraph 2", 8),
    SLAYER_ENDERMAN_3("§cVoidgloom Seraph 3", 8),
    SLAYER_ENDERMAN_4("§4Voidgloom Seraph 4", 8),

    BLAZE_SLAYER("Blaze Slayer", 9, "Blaze"),

    HUB_HEADLESS_HORSEMAN("§6Headless Horseman", 10),
}