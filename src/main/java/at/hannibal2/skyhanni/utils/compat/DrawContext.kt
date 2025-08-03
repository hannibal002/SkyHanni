package at.hannibal2.skyhanni.utils.compat

import io.github.notenoughupdates.moulconfig.internal.GlScissorStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.item.ItemStack

class DrawContext {
    private val _matrices = MatrixStack()
    val matrices: MatrixStack
        get() = _matrices

    fun drawText(fr: FontRenderer, text: String, x: Int, y: Int, color: Int, shadow: Boolean) {
        fr.drawString(text, x.toFloat(), y.toFloat(), color, shadow)
    }

    fun drawItem(item: ItemStack, x: Int, y: Int) {
        Minecraft.getMinecraft().renderItem.renderItemIntoGUI(item, x, y)
    }

    fun fill(left: Int, top: Int, right: Int, bottom: Int, color: Int) {
        GuiScreen.drawRect(left, top, right, bottom, color)
    }

    fun enableScissor(left: Int, top: Int, right: Int, bottom: Int) {
        GlScissorStack.push(
            left,
            top,
            right,
            bottom,
            ScaledResolution(Minecraft.getMinecraft()),
            false, // bypassInclusion
        )
    }

    fun disableScissor() {
        GlScissorStack.pop(ScaledResolution(Minecraft.getMinecraft()))
    }

}
