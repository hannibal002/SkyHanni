package at.hannibal2.hanni.features.fishing.trophy

import at.hannibal2.hanni.utils.StringUtils.firstLetterUppercase

enum class TrophyRarity(val formatCode: String) {
    BRONZE("§8"),
    SILVER("§7"),
    GOLD("§6"),
    DIAMOND("§b");

    val formattedString get() = "$formatCode${name.firstLetterUppercase()}"

    companion object {

        fun getByName(rawName: String) = entries.firstOrNull { rawName.uppercase().endsWith(it.name) }
    }
}
