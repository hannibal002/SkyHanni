package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.test.command.ErrorManager
import net.minecraft.item.EnumDyeColor
import java.awt.Color

enum class LorenzColor(val chatColorCode: Char, private val color: Color, private val coloredLabel: String) {
    BLACK('0', Color(0, 0, 0), "§0Black"),
    DARK_BLUE('1', Color(0, 0, 170), "§1Dark Blue"),
    DARK_GREEN('2', Color(0, 170, 0), "§2Dark Green"),
    DARK_AQUA('3', Color(0, 170, 170), "§3Dark Aqua"),
    DARK_RED('4', Color(170, 0, 0), "§4Dark Red"),
    DARK_PURPLE('5', Color(170, 0, 170), "§5Dark Purple"),
    GOLD('6', Color(255, 170, 0), "§6Gold"),
    GRAY('7', Color(170, 170, 170), "§7Gray"),
    DARK_GRAY('8', Color(85, 85, 85), "§8Dark Gray"),
    BLUE('9', Color(85, 85, 255), "§9Blue"),
    GREEN('a', Color(85, 255, 85), "§aGreen"),
    AQUA('b', Color(85, 255, 255), "§bAqua"),
    RED('c', Color(255, 85, 85), "§cRed"),
    LIGHT_PURPLE('d', Color(255, 85, 255), "§dLight Purple"),
    YELLOW('e', Color(255, 255, 85), "§eYellow"),
    WHITE('f', Color(255, 255, 255), "§fWhite"),
    CHROMA('Z', Color(0, 0, 0, 0), "§ZChroma") // If chroma, go transparent instead of color code.
    ;

    fun getChatColor(): String = "§$chatColorCode"

    fun toColor(): Color = color

    fun addOpacity(opacity: Int): Color {
        val color = toColor()
        val red = color.red
        val green = color.green
        val blue = color.blue
        return Color(red, green, blue, opacity)
    }

    override fun toString(): String = coloredLabel

    companion object {
        fun EnumDyeColor.toLorenzColor() = when (this) {
            EnumDyeColor.WHITE -> WHITE
            EnumDyeColor.MAGENTA -> LIGHT_PURPLE
            EnumDyeColor.PINK -> LIGHT_PURPLE
            EnumDyeColor.RED -> RED
            EnumDyeColor.SILVER -> GRAY
            EnumDyeColor.GRAY -> GRAY
            EnumDyeColor.GREEN -> DARK_GREEN
            EnumDyeColor.LIME -> GREEN
            EnumDyeColor.BLUE -> BLUE
            EnumDyeColor.PURPLE -> DARK_PURPLE
            EnumDyeColor.YELLOW -> YELLOW
            else -> {
                ErrorManager.logError(
                    Exception("Unknown dye color: $this"),
                    "Unknown dye color: $this"
                )
                null
            }
        }
    }
}
