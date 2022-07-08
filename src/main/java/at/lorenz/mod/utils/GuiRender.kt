package at.lorenz.mod.utils

import com.thatgravyboat.skyblockhud_2.core.config.Position
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager

object GuiRender {

    fun Position.renderString(string: String) {

        GlStateManager.pushMatrix()
        val resolution = ScaledResolution(Minecraft.getMinecraft())

        val renderer = Minecraft.getMinecraft().renderManager.fontRenderer

        val offsetX = (200 - renderer.getStringWidth(string)) / 2

        val x = getAbsX(resolution, 200) + offsetX
        val y = getAbsY(resolution, 16)



        GlStateManager.translate(x + 1.0, y + 1.0, 0.0)
        renderer.drawStringWithShadow(string, 0f, 0f, 0)


        GlStateManager.popMatrix()
    }
}