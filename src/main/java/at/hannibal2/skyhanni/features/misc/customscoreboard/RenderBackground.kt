package at.hannibal2.skyhanni.features.misc.customscoreboard

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.GuiEditManager
import at.hannibal2.skyhanni.data.GuiEditManager.Companion.getAbsX
import at.hannibal2.skyhanni.data.GuiEditManager.Companion.getAbsY
import at.hannibal2.skyhanni.data.GuiEditManager.Companion.getDummySize
import at.hannibal2.skyhanni.utils.SpecialColour
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11

private val config get() = SkyHanniMod.feature.gui.customScoreboard

class RenderBackground {
    fun renderBackground() {
        val position = config.position
        val border = 5

        val x = position.getAbsX()
        val y = position.getAbsY()

        val elementWidth = position.getDummySize().x
        val elementHeight = position.getDummySize().y

        val scaledWidth = ScaledResolution(Minecraft.getMinecraft()).scaledWidth
        val scaledHeight = ScaledResolution(Minecraft.getMinecraft()).scaledHeight

        position.set(
            Position(
                if (config.displayConfig.alignment.alignRight)
                    scaledWidth - elementWidth - (border * 2)
                else x,
                if (config.displayConfig.alignment.alignCenterVertically)
                    scaledHeight / 2 - elementHeight / 2
                else y,
                position.getScale(),
                position.isCenter
            )
        )

        if (GuiEditManager.isInGui()) return

        /*if (config.backgroundConfig.enabled) {
            ShaderManager.enableShader("rounded_rectangle")
        } else {
            ShaderManager.disableShader()
        }*/

        val textureLocation = ResourceLocation("skyhanni", "scoreboard.png")

        // Save the current color state
        GlStateManager.pushMatrix();
        GlStateManager.pushAttrib();

        GlStateManager.color(1f, 1f, 1f, 1f)

        if (config.backgroundConfig.enabled && config.backgroundConfig.useCustomBackgroundImage) {
            // Draw the default texture
            Minecraft.getMinecraft().textureManager.bindTexture(textureLocation)
            Utils.drawTexturedRect(
                (x - border).toFloat(),
                (y - border).toFloat(),
                (elementWidth + border * 3).toFloat(),
                (elementHeight + border * 2).toFloat(),
                GL11.GL_NEAREST
            )
        } else if (config.backgroundConfig.enabled) {
            // Draw a solid background with a specified color
            Gui.drawRect(
                x - border,
                y - border,
                x + elementWidth + border * 2,
                y + elementHeight + border,
                SpecialColour.specialToChromaRGB(config.backgroundConfig.color)
            )
        }

        // Restore the original color state
        GlStateManager.popMatrix();
        GlStateManager.popAttrib();
    }
}
