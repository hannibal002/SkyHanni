package at.hannibal2.skyhanni.config.core.elements

import io.github.notenoughupdates.moulconfig.common.RenderContext
import java.awt.Color

class GuiElementButton {

    var text: String = ""

    val height: Int = 18 + 5
    var width: Int = -1

    fun getWidth(context: RenderContext): Int {
        val fr = context.minecraft.defaultFontRenderer
        return fr.getStringWidth(text) + 10
    }

    fun render(context: RenderContext, x: Int, y: Int) {
        context.drawColoredRect(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + 18).toFloat(), Color.WHITE.rgb)
        context.drawColoredRect((x + 1).toFloat(), (y + 1).toFloat(), (x + width - 1).toFloat(), (y + 18 - 1).toFloat(), Color.BLACK.rgb)
        val fr = context.minecraft.defaultFontRenderer
        context.drawString(fr, text, x + 5, y + 5, -1, true)
    }
}
