package at.hannibal2.skyhanni.features.chroma

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.mixins.transformers.AccessorMinecraft
import at.hannibal2.skyhanni.utils.TickUtils
import at.hannibal2.skyhanni.utils.shader.Shader
import at.hannibal2.skyhanni.utils.shader.Uniform
import net.minecraft.client.Minecraft

/**
 * Modified from SkyblockAddons
 *
 * Credit: [ChromaShader.java](https://github.com/BiscuitDevelopment/SkyblockAddons/blob/main/src/main/java/codes/biscuit/skyblockaddons/shader/chroma/ChromaShader.java)
 */
object ChromaShader : Shader("chroma", "chroma") {

    val INSTANCE: ChromaShader
        get() = this

    override fun registerUniforms() {
        registerUniform(Uniform.UniformType.FLOAT, "chromaSize") {
            SkyHanniMod.feature.chroma.chromaSize * (Minecraft.getMinecraft().displayWidth / 100f)
        }
        registerUniform(Uniform.UniformType.FLOAT, "timeOffset") {
            val ticks = TickUtils.getTotalTicks() + (Minecraft.getMinecraft() as AccessorMinecraft).timer.renderPartialTicks
            val chromaSpeed = SkyHanniMod.feature.chroma.chromaSpeed / 360f
            ticks * chromaSpeed
        }
        registerUniform(Uniform.UniformType.FLOAT, "saturation") {
            SkyHanniMod.feature.chroma.chromaSaturation
        }
    }
}