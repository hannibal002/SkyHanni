package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.test.command.CopyErrorCommand


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
    VERY_SPECIAL(LorenzColor.RED, 9),
    ;

    fun oneBelow(logError: Boolean = true): LorenzRarity? {
        val rarityBelow = getById(ordinal - 1)
        if (rarityBelow == null && logError) {
            CopyErrorCommand.logErrorState(
                "Problem with item rarity detected.",
                "Trying to get an item rarity below common"
            )
        }
        return rarityBelow
    }

    fun oneAbove(logError: Boolean = true): LorenzRarity? {
        val rarityBelow = getById(ordinal + 1)
        if (rarityBelow == null && logError) {
            CopyErrorCommand.logErrorState(
                "Problem with item rarity detected.",
                "Trying to get an item rarity above special"
            )
        }
        return rarityBelow
    }

    companion object {
        fun getById(id: Int) = entries.firstOrNull { it.ordinal == id }
    }

}