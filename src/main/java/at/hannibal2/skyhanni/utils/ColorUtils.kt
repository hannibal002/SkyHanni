package at.hannibal2.skyhanni.utils

import net.minecraft.client.renderer.GlStateManager
import java.awt.Color

object ColorUtils {
    fun getRed(colour: Int) = colour shr 16 and 0xFF

    fun getGreen(colour: Int) = colour shr 8 and 0xFF

    fun getBlue(colour: Int) = colour and 0xFF

    fun getAlpha(colour: Int) = colour shr 24 and 0xFF

    fun blendRGB(start: Color, end: Color, percent: Double) = Color(
        (start.red * (1 - percent) + end.red * percent).toInt(),
        (start.green * (1 - percent) + end.green * percent).toInt(),
        (start.blue * (1 - percent) + end.blue * percent).toInt()
    )

    private fun bindColor(r: Int, g: Int, b: Int, a: Int, colorMultiplier: Float) {
        bindColor(r / 255f * colorMultiplier, g / 255f * colorMultiplier, b / 255f * colorMultiplier, a / 255f)
    }

    private fun bindColor(r: Float, g: Float, b: Float, a: Float) {
        GlStateManager.color(r, g, b, a)
    }

    fun bindColor(color: Int, colorMultiplier: Float) {
        bindColor(getRed(color), getGreen(color), getBlue(color), getAlpha(color), colorMultiplier)
    }
}
