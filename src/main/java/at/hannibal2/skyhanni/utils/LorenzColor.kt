package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.test.command.ErrorManager
import net.minecraft.item.EnumDyeColor
import java.awt.Color

enum class LorenzColor(private var chatColorCode: Char, private val color: Color) {
    BLACK('0', Color(0, 0, 0)),
    DARK_BLUE('1', Color(0, 0, 170)),
    DARK_GREEN('2', Color(0, 170, 0)),
    DARK_AQUA('3', Color(0, 170, 170)),
    DARK_RED('4', Color(170, 0, 0)),
    DARK_PURPLE('5', Color(170, 0, 170)),
    GOLD('6', Color(255, 170, 0)),
    GRAY('7', Color(170, 170, 170)),
    DARK_GRAY('8', Color(85, 85, 85)),
    BLUE('9', Color(85, 85, 255)),
    GREEN('a', Color(85, 255, 85)),
    AQUA('b', Color(85, 255, 255)),
    RED('c', Color(255, 85, 85)),
    LIGHT_PURPLE('d', Color(255, 85, 255)),
    YELLOW('e', Color(255, 255, 85)),
    WHITE('f', Color(255, 255, 255)),
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