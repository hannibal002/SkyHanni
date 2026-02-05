package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.chat.TextHelper
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import java.util.Optional

// TODO: replace id with ordinal
enum class LorenzRarity(val color: LorenzColor, val id: Int) {

    COMMON(LorenzColor.WHITE, 0),
    UNCOMMON(LorenzColor.GREEN, 1),
    RARE(LorenzColor.BLUE, 2),
    EPIC(LorenzColor.DARK_PURPLE, 3),
    LEGENDARY(LorenzColor.GOLD, 4),
    MYTHIC(LorenzColor.LIGHT_PURPLE, 5),
    DIVINE(LorenzColor.AQUA, 6),
    SPECIAL(LorenzColor.RED, 8),
    VERY_SPECIAL(LorenzColor.RED, 9),
    ULTIMATE(LorenzColor.DARK_RED, 10),
    ;

    val chatColorCode get() = color.getChatColor()
    val rawName = name.replace("_", " ")
    val formattedName = rawName.firstLetterUppercase()

    fun oneBelow(logError: Boolean = true): LorenzRarity? {
        val rarityBelow = getById(ordinal - 1)
        if (rarityBelow == null && logError) {
            ErrorManager.logErrorStateWithData(
                "Problem with item rarity detected.",
                "Trying to get an item rarity below common",
                "ordinal" to ordinal,
            )
        }
        return rarityBelow
    }

    fun oneAbove(logError: Boolean = true): LorenzRarity? {
        val rarityBelow = getById(ordinal + 1)
        if (rarityBelow == null && logError) {
            ErrorManager.logErrorStateWithData(
                "Problem with item rarity detected.",
                "Trying to get an item rarity above special",
                "ordinal" to ordinal,
            )
        }
        return rarityBelow
    }

    fun isAtLeast(other: LorenzRarity): Boolean = this.ordinal >= other.ordinal

    companion object {

        fun getById(id: Int) = if (entries.size > id) entries[id] else null

        fun getByName(name: String): LorenzRarity? = entries.find { it.name.equals(name, ignoreCase = true) }

        fun getByNameOrError(name: String): LorenzRarity = getByName(name) ?: error("LorenzRarity not found by name: '$name'")

        fun getByColorCode(colorCode: Char): LorenzRarity? = entries.find { it.color.chatColorCode == colorCode }

        fun getByComponent(component: Component, stringMatch: String): LorenzRarity? {
            var rarity: LorenzRarity? = null
            TextHelper.matcher(component, stringMatch)?.visit({ style: Style?, string: String? ->
                if (string == stringMatch) {
                    rarity = when {
                        (style?.color?.name == "dark_red") -> ULTIMATE
                        (style?.color?.name == "red") -> SPECIAL // special and very special are the same name
                        (style?.color?.name == "aqua") -> DIVINE
                        (style?.color?.name == "light_purple") -> MYTHIC
                        (style?.color?.name == "gold") -> LEGENDARY
                        (style?.color?.name == "dark_purple") -> EPIC
                        (style?.color?.name == "blue") -> RARE
                        (style?.color?.name == "green") -> UNCOMMON
                        (style?.color?.name == "white") -> COMMON
                        else -> null
                    }
                }
                Optional.empty<Component>()
            }, Style.EMPTY)
            return rarity
        }
    }
}
