package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.utils.StringUtils.removeColor

enum class VisitorRarity(val displayName: String) {
    UNCOMMON("§aUncommon"),
    RARE("§9Rare"),
    LEGENDARY("§6Legendary"),
    MYTHIC("§dMythic"),
    SPECIAL("§cSpecial"),
    UNKNOWN("")
    ;

    override fun toString(): String = displayName

    companion object {
        val filterableEntries by lazy { VisitorRarity.entries.filter { it.displayName.isNotEmpty() } }

        fun getByNameOrNull(itemName: String): VisitorRarity? {
            return VisitorRarity.entries.firstOrNull {
                it.displayName.removeColor().equals(itemName.removeColor(), ignoreCase = true)
            }
        }

        fun getFromColorCode(char: Char): VisitorRarity {
            return when (char) {
                'a' -> UNCOMMON
                '9' -> RARE
                '6' -> LEGENDARY
                'd' -> MYTHIC
                'c' -> SPECIAL
                else -> UNKNOWN
            }
        }
    }
}
