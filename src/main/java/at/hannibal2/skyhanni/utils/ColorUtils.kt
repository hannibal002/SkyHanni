package at.hannibal2.skyhanni.utils

import java.awt.Color
import kotlin.math.max


object ColorUtils {

    /** transfer string colors from the config to java.awt.Color */
    fun String.toChromaColor() = Color(SpecialColour.specialToChromaRGB(this), true)
    fun getRed(colour: Int) = colour shr 16 and 0xFF

    fun getGreen(colour: Int) = colour shr 8 and 0xFF

    fun getBlue(colour: Int) = colour and 0xFF

    fun getAlpha(colour: Int) = colour shr 24 and 0xFF

    fun blendRGB(start: Color, end: Color, percent: Double) = Color(
        (start.red * (1 - percent) + end.red * percent).toInt(),
        (start.green * (1 - percent) + end.green * percent).toInt(),
        (start.blue * (1 - percent) + end.blue * percent).toInt()
    )

    fun Color.darker(factor: Double): Color {
        return Color(
            max((red * factor).toInt(), 0),
            max((green * factor).toInt(), 0),
            max((blue * factor).toInt(), 0),
            alpha)
    }
}
