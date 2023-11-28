package at.hannibal2.skyhanni.features.misc.customscoreboard

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.core.config.Position
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
private var cooldown = 0

class RenderBackground {
    fun renderBackground(){
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
                when (config.displayConfig.alignRight) {
                    true -> scaledWidth - elementWidth - (border * 2)
                    false -> x
                },
                when (config.displayConfig.alignCenterVertically) {
                    true -> scaledHeight / 2 - elementHeight / 2
                    false -> y
                },
                position.getScale(),
                position.isCenter
            )
        )
        /*if (config.backgroundConfig.enabled) {
            ShaderManager.enableShader("rounded_rectangle")
        } else {
            ShaderManager.disableShader()
        }*/

        val textureLocation = ResourceLocation("skyhanni", "scoreboard.png")
        val rareTextureLocation = ResourceLocation("skyhanni", "rareScoreboardBackground.png")

        // Save the current color state
        GlStateManager.pushMatrix();
        GlStateManager.pushAttrib();

        GlStateManager.color(1f,1f,1f, 1f)

        if (config.backgroundConfig.enabled && config.backgroundConfig.useCustomBackgroundImage) {
            if (cooldown > 0) {
                cooldown--

                // Display rare texture during cooldown
                Minecraft.getMinecraft().textureManager.bindTexture(rareTextureLocation)
                Utils.drawTexturedRect(
                    (x - border).toFloat(),
                    (y - border).toFloat(),
                    (elementWidth + border * 3).toFloat(),
                    (elementHeight + border * 3).toFloat(),
                    GL11.GL_NEAREST
                )
            } else if (Math.random() * 86400.0 * 20 == 1.0) {
                // Randomly switch to rare texture with a 1 in 86400 chance (once per day)
                Minecraft.getMinecraft().textureManager.bindTexture(rareTextureLocation)
                cooldown = 200

                Utils.drawTexturedRect(
                    (x - border).toFloat(),
                    (y - border).toFloat(),
                    (elementWidth + border * 2).toFloat(),
                    (elementHeight + border * 2).toFloat(),
                    GL11.GL_NEAREST
                )
            } else {
                // Draw the default texture
                Minecraft.getMinecraft().textureManager.bindTexture(textureLocation)
                Utils.drawTexturedRect(
                    (x - border).toFloat(),
                    (y - border).toFloat(),
                    (elementWidth + border * 3).toFloat(),
                    (elementHeight + border * 3).toFloat(),
                    GL11.GL_NEAREST
                )
            }
        } else if (config.backgroundConfig.enabled) {
            // Draw a solid background with a specified color
            Gui.drawRect(
                x - border,
                y - border,
                x + elementWidth + border * 2,
                y + elementHeight + border * 2,
                SpecialColour.specialToChromaRGB(config.backgroundConfig.color)
            )
        }

        // Restore the original color state
        GlStateManager.popMatrix();
        GlStateManager.popAttrib();
    }
}
