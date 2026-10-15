package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.chat.TextHelper
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import java.util.Optional

enum class LorenzRarity(val color: LorenzColor) {

    COMMON(WHITE),
    UNCOMMON(GREEN),
    RARE(BLUE),
    EPIC(DARK_PURPLE),
    LEGENDARY(GOLD),
    MYTHIC(LIGHT_PURPLE),
    DIVINE(AQUA),
    SPECIAL(RED),
    VERY_SPECIAL(RED),
    ULTIMATE(DARK_RED),
    ;

    val chatColorCode get() = color.getChatColor()
    val rawName = name.replace("_", " ")
    val formattedName = rawName.firstLetterUppercase()

    /** The number NEU pet internal names end in, e.g. `PHOENIX;7` is Special. */
    val neuPetIndex: Int get() = ordinal

    fun oneBelow(logError: Boolean = true): LorenzRarity? {
        val rarityBelow = entries.getOrNull(ordinal - 1)
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
        val rarityAbove = entries.getOrNull(ordinal + 1)
        if (rarityAbove == null && logError) {
            ErrorManager.logErrorStateWithData(
                "Problem with item rarity detected.",
                "Trying to get an item rarity above ultimate",
                "ordinal" to ordinal,
            )
        }
        return rarityAbove
    }

    fun isAtLeast(other: LorenzRarity): Boolean = this.ordinal >= other.ordinal

    companion object {

        /** Reverse of [LorenzRarity.neuPetIndex], null when out of range. */
        fun fromNeuPetIndex(index: Int): LorenzRarity? = entries.getOrNull(index)

        fun getByName(name: String): LorenzRarity? = entries.find { it.name.equals(name, ignoreCase = true) }

        fun getByNameOrError(name: String): LorenzRarity = getByName(name) ?: error("LorenzRarity not found by name: '$name'")

        fun getByColorCode(colorCode: Char): LorenzRarity? = entries.find { it.color.chatColorCode == colorCode }

        fun getByComponent(component: Component, stringMatch: String): LorenzRarity? {
            var rarity: LorenzRarity? = null
            TextHelper.matcher(component, stringMatch)?.visit({ style: Style, string: String ->
                if (string == stringMatch) {
                    rarity = when (style.color?.name) {
                        "dark_red" -> ULTIMATE
                        "red" -> SPECIAL // special and very special are the same name
                        "aqua" -> DIVINE
                        "light_purple" -> MYTHIC
                        "gold" -> LEGENDARY
                        "dark_purple" -> EPIC
                        "blue" -> RARE
                        "green" -> UNCOMMON
                        "white" -> COMMON
                        else -> null
                    }
                }
                Optional.empty<Component>()
            }, Style.EMPTY)
            return rarity
        }
    }
}
