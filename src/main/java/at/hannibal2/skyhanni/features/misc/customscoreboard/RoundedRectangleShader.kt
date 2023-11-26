package at.hannibal2.skyhanni.features.misc.customscoreboard

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.GuiEditManager.Companion.getDummySize
import at.hannibal2.skyhanni.utils.SpecialColour
import at.hannibal2.skyhanni.utils.shader.Shader
import at.hannibal2.skyhanni.utils.shader.Uniform
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
/*
    DOES NOT WORK, maybe someone can fix the rendering because I have no idea what the heck is going on ~ J10a1n15
 */
private val config get() = SkyHanniMod.feature.gui.customScoreboard

object RoundedRectangleShader : Shader("rounded_rectangle", "rounded_rectangle") {

    val INSTANCE: RoundedRectangleShader
        get() = this

    override fun registerUniforms() {
        val size = config.position.getDummySize()
        val screenWidth = ScaledResolution(Minecraft.getMinecraft()).scaledWidth
        val screenHeight = ScaledResolution(Minecraft.getMinecraft()).scaledHeight
        val border = 5
        registerUniform(Uniform.UniformType.VEC2, "v_texCoord") {
            floatArrayOf(((size.x - border) / screenWidth).toFloat(), ((size.y - border) / screenHeight).toFloat())
        }

        registerUniform(Uniform.UniformType.FLOAT, "width") {
            ((size.x + border * 2) / screenWidth).toFloat()
        }

        registerUniform(Uniform.UniformType.FLOAT, "height") {
            ((size.y + border * 2) / screenHeight).toFloat()
        }

        registerUniform(Uniform.UniformType.FLOAT, "roundness") {
            0.05f
        }

        registerUniform(Uniform.UniformType.VEC4, "color") {
            val color: Int = SpecialColour.specialToChromaRGB(config.backgroundConfig.color)
            floatArrayOf(
                ((color shr 16 and 255) / 255.0f),
                ((color shr 8 and 255) / 255.0f),
                ((color and 255) / 255.0f),
                ((color shr 24 and 255) / 255.0f)
            )
        }
    }
}
