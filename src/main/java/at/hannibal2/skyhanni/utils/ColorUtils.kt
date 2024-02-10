package at.hannibal2.skyhanni.utils

import java.awt.Color

object ColorUtils {

    /** transfer string colors from the config to java.awt.Color */
    fun String.toChromaColor() = Color(SpecialColour.specialToChromaRGB(this), true)
    fun getRed(colour: Int) = colour shr 16 and 0xFF

    fun getGreen(colour: Int) = colour shr 8 and 0xFF

    fun getBlue(colour: Int) = colour and 0xFF

    fun getAlpha(colour: Int) = colour shr 24 and 0xFF
}
