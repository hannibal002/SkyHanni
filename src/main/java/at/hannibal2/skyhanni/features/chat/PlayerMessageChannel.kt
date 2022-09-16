package at.hannibal2.skyhanni.features.chat

enum class PlayerMessageChannel(
    val prefixColor: String,
    val prefixSmall: String,
    val prefixLarge: String,
    val originalPrefix: String,
) {

    ALL("§f", "A", "All", ""),
    ALL_GUESTING("§a", "g", "Guest", "§a[✌] "),
    ALL_DUNGEON_DEAD("§7", "D", "Dead", "§7[GHOST] "),
    PARTY("§9", "P", "Party", "§9Party §8> "),
    GUILD("§2", "G", "Guild", "§2Guild > "),
    COOP("§b", "CC", "Co-op", "§bCo-op > "),

}