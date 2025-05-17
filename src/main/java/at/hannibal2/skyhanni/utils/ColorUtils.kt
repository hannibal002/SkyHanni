package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import io.github.notenoughupdates.moulconfig.ChromaColour
import java.awt.Color

object ColorUtils {

    @JvmStatic
    @JvmOverloads
    fun Color.toChromaColor(alpha: Int = this.alpha, chroma: Int = 0): ChromaColour =
        ChromaColour.fromRGB(red, green, blue, alpha, chroma)

    @JvmStatic
    fun String.toChromaColor() = ChromaColour.forLegacyString(this)

    fun ChromaColour.toColor(): Color = Color(toInt(), true)

    // TODO: Replace this code with the call to moulconfig's function once its fixed. revert #3821
    fun ChromaColour.toInt(): Int {
        val effectiveHue: Double
        if (timeForFullRotationInMillis > 0) {
            effectiveHue = System.currentTimeMillis() / timeForFullRotationInMillis.toDouble()
        } else {
            effectiveHue = hue.toDouble()
        }

        val rgb = Color.HSBtoRGB((effectiveHue % 1.0).toFloat(), this.saturation, this.brightness)
        return (alpha and 0xFF) shl 24 or (rgb and 0xFFFFFF)
    }

    fun String.getFirstColorCode() = takeIf { it.firstOrNull() == '§' }?.getOrNull(1)

    fun getAlpha(color: Int) = color shr 24 and 0xFF

    fun getRed(color: Int) = color shr 16 and 0xFF

    fun getGreen(color: Int) = color shr 8 and 0xFF

    fun getBlue(color: Int) = color and 0xFF

    private val tooltipFixBool get() = SkyHanniMod.feature.misc.transparentTooltips

    // I think you need to manually import these
    operator fun Color.component1(): Float = if (!tooltipFixBool) this.alpha / 255f else this.red / 255f
    operator fun Color.component2(): Float = if (!tooltipFixBool) this.red / 255f else this.green / 255f
    operator fun Color.component3(): Float = if (!tooltipFixBool) this.green / 255f else this.blue / 255f
    operator fun Color.component4(): Float = if (!tooltipFixBool) this.blue / 255f else this.alpha / 255f


    fun blendRGB(start: Color, end: Color, percent: Double) = Color(
        (start.red * (1 - percent) + end.red * percent).toInt(),
        (start.green * (1 - percent) + end.green * percent).toInt(),
        (start.blue * (1 - percent) + end.blue * percent).toInt(),
    )

    fun Color.getExtendedColorCode(hasAlpha: Boolean = false): String = ExtendedChatColor(rgb, hasAlpha).toString()

    /** Darkens a color by a [factor]. The lower the [factor], the darker the color. */
    fun Color.darker(factor: Double = 0.7) = Color(
        (red * factor).toInt().coerceIn(0, 255),
        (green * factor).toInt().coerceIn(0, 255),
        (blue * factor).toInt().coerceIn(0, 255),
        alpha,
    )

    val TRANSPARENT_COLOR = Color(0, 0, 0, 0)

    fun Color.addAlpha(alpha: Int): Color = Color(red, green, blue, alpha)

    fun getColorFromHex(hex: String): Int = runCatching { Color(Integer.decode(hex)) }.getOrNull()?.rgb ?: 0

    // copied from minecraft, thx jappa
    private val colorCodes = makeColorCodeArray()

    private fun makeColorCodeArray(): IntArray {
        val colorCode = IntArray(32)
        for (i in 0..31) {
            val j = (i shr 3 and 1) * 85
            var k = (i shr 2 and 1) * 170 + j
            var l = (i shr 1 and 1) * 170 + j
            var i1 = (i shr 0 and 1) * 170 + j
            if (i == 6) {
                k += 85
            }

            if (i >= 16) {
                k /= 4
                l /= 4
                i1 /= 4
            }

            colorCode[i] = ((k and 0xFF) shl 16) or ((l and 0xFF) shl 8) or (i1 and 0xFF)
        }
        return colorCode
    }

    fun getColorCode(color: Char): Int {
        return colorCodes["0123456789abcdef".indexOf(color)]
    }
}
