package at.hannibal2.skyhanni.utils

import java.awt.Color

object ColorUtils {
    fun getRed(colour: Int) = colour shr 16 and 0xFF

    fun getGreen(colour: Int) = colour shr 8 and 0xFF

    fun getBlue(colour: Int) = colour and 0xFF

    fun getAlpha(colour: Int) = colour shr 24 and 0xFF

    fun blend(start: Color, end: Color, percent: Double) = Color(
        (start.getRed() * (1 - percent) + end.getRed() * percent).toInt(),
        (start.getGreen() * (1 - percent) + end.getGreen() * percent).toInt(),
        (start.getBlue() * (1 - percent) + end.getBlue() * percent).toInt()
    )
}
