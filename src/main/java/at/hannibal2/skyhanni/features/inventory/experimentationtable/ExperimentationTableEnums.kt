package at.hannibal2.skyhanni.features.inventory.experimentationtable

enum class ExperimentMessages(private val displayName: String) {
    DONE("§eYou claimed the §dSuperpairs §erewards! §8(§7Claim§8)"),
    EXPERIENCE("§8 +§3141k Experience §8(§7Experience Drops§8)"),
    ENCHANTMENTS("§8 +§9Smite VII §8(§7Enchantment Drops§8)"),
    BOTTLES("§8 +§9Titanic Experience Bottle §8(§7Bottle Drops§8)"),
    MISC("§8 +§5Metaphysical Serum §8(§7Misc Drops§8)");

    override fun toString() = displayName
}

enum class ExperimentTaskType(private val displayName: String) {
    CHRONOMATRON("Chronomatron"),
    ULTRASEQUENCER("Ultrasequencer"),
    SUPERPAIRS("Superpairs"),
    ;

    override fun toString() = displayName
}

enum class ExperimentTier(
    private val displayName: String,
    overInclusiveSlotRange: IntRange, // Filtered 'later' to remove side spaces
    private val sideSpace: Int = 1
) {
    NONE("",  0..0, sideSpace = 0),
    BEGINNER("Beginner", 18..35),
    HIGH("High", 10..43, sideSpace = 2),
    GRAND("Grand",  10..43, sideSpace = 2),
    SUPREME("Supreme",  9..44),
    TRANSCENDENT("Transcendent",  9..44),
    METAPHYSICAL("Metaphysical",  9..44),
    ;

    val slotRange = overInclusiveSlotRange.filter {
        (it % 9) !in when (sideSpace) {
            1 -> listOf(0, 8)
            2 -> listOf(0, 1, 7, 8)
            else -> emptyList()
        }
    }

    val gridSize: Int = slotRange.size

    override fun toString() = displayName

    companion object {
        fun byNameOrNone(name: String): ExperimentTier = entries.firstOrNull {
            it.displayName.equals(name, ignoreCase = true)
        } ?: NONE
    }
}
