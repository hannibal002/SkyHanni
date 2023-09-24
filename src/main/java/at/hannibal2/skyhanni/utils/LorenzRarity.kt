package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.test.command.CopyErrorCommand
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import net.minecraft.item.ItemStack


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

    private val chatColorCode by lazy { color.getChatColor() }
    private val rawName by lazy { name.replace("_", " ") }
    private val normalName by lazy { "$chatColorCode§l$rawName" }
    private val recombName by lazy { "$chatColorCode§l§ka§r $chatColorCode§l$chatColorCode§l$rawName" }

    //§d§l§ka§r §d§l§d§lMYTHIC

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

        fun readItemRarity(itemStack: ItemStack): LorenzRarity? {
            for (line in itemStack.getLore()) {
                for (rarity in LorenzRarity.entries) {
                    if (line.startsWith(rarity.normalName) || line.startsWith(rarity.recombName)) {
                        return rarity
                    }
                }
            }
            return null
        }
    }

}