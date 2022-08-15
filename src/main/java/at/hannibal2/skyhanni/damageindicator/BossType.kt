package at.hannibal2.skyhanni.damageindicator

enum class BossType(val bossName: String) {
    DUNGEON("Generic Dungeon boss"),//TODO split in different bosses

    NETHER_BLADESOUL("§8Bladesoul"),
    NETHER_MAGMA_BOSS("§4Magma Boss"),
    NETHER_ASHFANG("§cAshfang"),
    NETHER_BARBARIAN_DUKE("§eBarbarian Duke"),
    NETHER_MAGE_OUTLAW("§5Mage Outlaw"),

    NETHER_VANQUISHER("§5Vanquisher"),

    END_ENDSTONE_PROTECTOR("§cEndstone Protector"),//TODO add color
    END_ENDERMAN_SLAYER("Voidgloom Seraph"),//TODO use seperate enums per tier
    END_ENDER_DRAGON("Ender Dragon"),//TODO fix totally

    HUB_REVENANT_HORROR("§5Revenant Horror 5"),//TODO add other variants like voidgloom?
    HUB_HEADLESS_HORSEMAN("§6Headless Horseman"),
}