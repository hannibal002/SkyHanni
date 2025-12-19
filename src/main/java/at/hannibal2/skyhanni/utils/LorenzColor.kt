package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import io.github.notenoughupdates.moulconfig.ChromaColour
import net.minecraft.ChatFormatting
import net.minecraft.world.item.DyeColor
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

    val next by lazy {
        when (this) {
            WHITE -> BLACK
            CHROMA -> BLACK
            else -> {
                val index = entries.indexOf(this)
                entries[index + 1]
            }
        }
    }

    fun getChatColor(): String = "§$chatColorCode"

    // TODO make this public fun unnecesary, replace with chroma color
    fun toColor(): Color = color

    // TODO make this functin return moulconfig.ChromaColour, and eventually remove awt.Color support
    fun addOpacity(opacity: Int): Color {
        val color = toColor()
        val red = color.red
        val green = color.green
        val blue = color.blue
        return Color(red, green, blue, opacity)
    }

    override fun toString(): String = coloredLabel

    private val cachedChromaColor by lazy { color.toChromaColor(this.color.alpha, 0) }

    // TODO make deprecated
    @JvmOverloads
    fun toChromaColor(alpha: Int = this.color.alpha, chromaSpeedMillis: Int = 0): ChromaColour {
        if (alpha == this.color.alpha && chromaSpeedMillis == 0) {
            return cachedChromaColor
        }
        return color.toChromaColor(alpha, chromaSpeedMillis)
    }

    fun toDyeColor(): DyeColor = when (this) {
        WHITE -> DyeColor.WHITE
        GOLD -> DyeColor.ORANGE
        AQUA -> DyeColor.MAGENTA
        BLUE -> DyeColor.LIGHT_BLUE
        YELLOW -> DyeColor.YELLOW
        GREEN -> DyeColor.LIME
        LIGHT_PURPLE -> DyeColor.PINK
        DARK_GRAY -> DyeColor.GRAY
        GRAY -> DyeColor.LIGHT_GRAY
        DARK_AQUA -> DyeColor.CYAN
        DARK_PURPLE -> DyeColor.PURPLE
        DARK_BLUE -> DyeColor.BLUE
//         GOLD -> EnumDyeColor.BROWN
        DARK_GREEN -> DyeColor.GREEN
        DARK_RED -> DyeColor.RED
        BLACK -> DyeColor.BLACK
        RED -> DyeColor.RED

        CHROMA -> DyeColor.WHITE
    }

    fun toChatFormatting(): ChatFormatting =
        ChatFormatting.entries.firstOrNull { it.toString() == getChatColor() } ?: ChatFormatting.WHITE

    companion object {

        fun DyeColor.toLorenzColor() = when (this) {
            DyeColor.WHITE -> WHITE
            DyeColor.MAGENTA -> LIGHT_PURPLE
            DyeColor.PINK -> LIGHT_PURPLE
            DyeColor.RED -> RED
            DyeColor.LIGHT_GRAY -> GRAY
            DyeColor.GRAY -> GRAY
            DyeColor.GREEN -> DARK_GREEN
            DyeColor.LIME -> GREEN
            DyeColor.BLUE -> BLUE
            DyeColor.PURPLE -> DARK_PURPLE
            DyeColor.YELLOW -> YELLOW
            DyeColor.ORANGE -> GOLD
            DyeColor.LIGHT_BLUE -> BLUE
            DyeColor.CYAN -> DARK_AQUA
            DyeColor.BROWN -> GOLD
            DyeColor.BLACK -> BLACK
        }

        fun Char.toLorenzColor(): LorenzColor? = entries.firstOrNull { it.chatColorCode == this } ?: run {
            ErrorManager.logErrorWithData(
                Exception("Unknown chat color: $this"),
                "Unknown chat color: $this"
            )
            null
        }
    }
}
