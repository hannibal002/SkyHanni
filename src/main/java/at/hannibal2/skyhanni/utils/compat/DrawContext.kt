package at.hannibal2.skyhanni.utils.compat

import io.github.moulberry.notenoughupdates.core.GlScissorStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution

class DrawContext {
    private val _matrices = MatrixStack()
    val matrices: MatrixStack
        get() = _matrices

    fun drawText(fr: FontRenderer, text: String, x: Int, y: Int, color: Int, shadow: Boolean) {
        fr.drawString(text, x.toFloat(), y.toFloat(), color, shadow)
    }

    fun fill(left: Int, top: Int, right: Int, bottom: Int, color: Int) {
        GuiScreen.drawRect(left, top, right, bottom, color)
    }

    fun enableScissor(left: Int, top: Int, right: Int, bottom: Int) {
        GlScissorStack.push(left, top, right, bottom, ScaledResolution(Minecraft.getMinecraft()))
    }

    fun disableScissor() {
        GlScissorStack.pop(ScaledResolution(Minecraft.getMinecraft()))
    }

}
