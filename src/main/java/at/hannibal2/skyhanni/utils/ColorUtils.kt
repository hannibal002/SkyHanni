package at.hannibal2.skyhanni.utils

object ColorUtils {
    fun getRed(colour: Int) = colour shr 16 and 0xFF

    fun getGreen(colour: Int) = colour shr 8 and 0xFF

    fun getBlue(colour: Int) = colour and 0xFF

    fun getAlpha(colour: Int) = colour shr 24 and 0xFF
}