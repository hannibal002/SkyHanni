package at.hannibal2.skyhanni.utils.compat

import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.GuiScreen

class DrawContext {
    private val _matrices = MatrixStack()
    val matrices: MatrixStack
        get() = _matrices

    fun drawString(fr: FontRenderer, text: String, x: Int, y: Int, color: Int, shadow: Boolean) {
        fr.drawString(text, x.toFloat(), y.toFloat(), color, shadow)
    }

    fun fill(left: Int, top: Int, right: Int, bottom: Int, color: Int) {
        GuiScreen.drawRect(left, top, right, bottom, color)
    }

}
