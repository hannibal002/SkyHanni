package at.hannibal2.skyhanni.features.event.hoppity

enum class HoppityRabbitRarity(val displayName: String, val colorCode: String) {
    COMMON("Common", "§f"),
    UNCOMMON("Uncommon", "§a"),
    RARE("Rare", "§9"),
    EPIC("Epic", "§5"),
    LEGENDARY("Legendary", "§6"),
    MYTHIC("Mythic", "§d"),
    DIVINE("Divine", "§b")
    ;

    companion object {
        fun getByRabbit(rabbitName: String): HoppityRabbitRarity? = entries.firstOrNull { it.colorCode == rabbitName.substring(0, 2) }
        fun getByKey(rarityKey: String): HoppityRabbitRarity? = entries.firstOrNull { it.displayName.lowercase() == rarityKey }
    }

}
