package at.hannibal2.skyhanni.utils


// TODO: replace id with ordinal
enum class LorenzRarity(val color: LorenzColor, val id: Int) {
    COMMON(LorenzColor.WHITE, 0),
    UNCOMMON(LorenzColor.GREEN, 1),
    RARE(LorenzColor.BLUE, 2),
    EPIC(LorenzColor.DARK_PURPLE, 3),
    LEGENDARY(LorenzColor.GOLD, 4),
    MYTHIC(LorenzColor.LIGHT_PURPLE, 5),
    DIVINE(LorenzColor.AQUA, 6),
    SUPREME(LorenzColor.DARK_RED, 7),
    SPECIAL(LorenzColor.RED, 8),
    ;

    companion object {
        fun getById(id: Int) = entries.firstOrNull { it.id == id }
    }

}