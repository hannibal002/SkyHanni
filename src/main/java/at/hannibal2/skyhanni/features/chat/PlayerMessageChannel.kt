package at.hannibal2.skyhanni.features.chat

enum class PlayerMessageChannel(val prefix: String, val originalPrefix: String) {

    ALL("§fA>", ""),
    ALL_GUESTING("§aA>", "§a[✌] "),
    ALL_DUNGEON_DEAD("§7A>", "§7[GHOST] "),
    PARTY("§9P>", "§9Party §8> "),
    GUILD("§2G>", "§2Guild > "),
    COOP("§bCC>", "§bCo-op > "),

}