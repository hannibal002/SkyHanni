package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.item.ItemStack

enum class Rarity(val colorCode: Char, val color: LorenzColor) {
    COMMON('f', LorenzColor.WHITE),
    UNCOMMON('a', LorenzColor.GREEN),
    RARE('9', LorenzColor.BLUE),
    EPIC('5', LorenzColor.DARK_PURPLE),
    LEGENDARY('6', LorenzColor.GOLD),
    MYTHIC('d', LorenzColor.LIGHT_PURPLE),
    DIVINE('b', LorenzColor.AQUA),
    SUPREME('4', LorenzColor.DARK_RED),
    SPECIAL('c', LorenzColor.RED),
    VERY_SPECIAL('c', LorenzColor.RED),
    UNKNOWN('0', LorenzColor.BLACK);

    companion object {
        fun ItemStack.getItemRarity(): Rarity? {
            val colorCode = this.getLore().lastOrNull()?.take(2)?.last()
            if (colorCode == 'c') {
                when (this.getLore().lastOrNull()?.removeColor()) {
                    "Special" -> SPECIAL
                    "Very Special" -> VERY_SPECIAL
                    else -> UNKNOWN
                }
            }
            return Rarity.values().firstOrNull { it.colorCode == colorCode }
        }

        fun ItemStack.getRecombRarityIndex(): Int {
            val index = Rarity.values().firstOrNull {
                it.name.lowercase() == this.getItemRarity()?.name?.lowercase()
            }?.ordinal ?: -2
            return index + 1
        }
    }
}