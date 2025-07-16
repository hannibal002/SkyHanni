package de.hype.bingonet.shared.constants

import java.awt.Color

enum class Formatting(val mCCode: String, @JvmField val discordFormattingCode: String, val color: Color?) {
    BLACK("§0", "\u001b[30m", Color(0x000000)),
    DARK_BLUE("§1", "\u001b[34m", Color(0x0000AA)),
    DARK_GREEN("§2", "\u001b[32m", Color(0x00AA00)),
    DARK_AQUA("§3", "\u001b[36m", Color(0x00AAAA)),
    DARK_RED("§4", "\u001b[31m", Color(0xAA0000)),
    DARK_PURPLE("§5", "\u001b[35m", Color(0xAA00AA)),
    GOLD("§6", "\u001b[33m", Color(0xFFAA00)),
    GRAY("§7", "\u001b[37m", Color(0xAAAAAA)),
    DARK_GRAY("§8", "\u001b[30m", Color(0x555555)),
    BLUE("§9", "\u001b[34m", Color(0x5555FF)),
    GREEN("§a", "\u001b[32m", Color(0x55FF55)),
    AQUA("§b", "\u001b[36m", Color(0x55FFFF)),
    RED("§c", "\u001b[31m", Color(0xFF5555)),
    LIGHT_PURPLE("§d", "\u001b[35m", Color(0xFF55FF)),
    YELLOW("§e", "\u001b[33m", Color(0xFFFF55)),
    WHITE("§f", "\u001b[37m", Color(0xFFFFFF)),
    BOLD("§l", "\u001b[1m", null),
    STRIKETHROUGH("§m", "\u001b[9m", null),
    UNDERLINE("§n", "\u001b[4m", null),
    ITALIC("§o", "\u001b[3m", null),
    OBFUSCATED("§k", "\u001b[6m", null),
    RESET("§r", "\u001b[0m", WHITE.color);

    override fun toString(): String {
        return this.mCCode
    }

    /**
     * short for discordFormattingCode
     */
    fun dc(): String {
        return discordFormattingCode
    }

    companion object {
        @JvmField
        var MC_FORMAT_REGEX: String = "§[0-9a-fk-or]"

        @JvmStatic
        fun covertToDiscordAnsi(string: String): String {
            var goalString = string
            for (formatting in entries) {
                goalString = goalString.replace(formatting.mCCode.toRegex(), formatting.discordFormattingCode)
            }
            return goalString
        }

        fun fromMCCode(value: String?): Formatting? {
            if (value == null) return null
            return entries.firstOrNull { it.mCCode == value }
                ?: throw IllegalArgumentException("No Formatting found with mCCode: $value")
        }

        fun getByColour(itemColor: Int): de.hype.bingonet.shared.constants.Formatting? {
            return entries.firstOrNull { it.color?.rgb == itemColor }
        }
    }
}
