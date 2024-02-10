package at.hannibal2.skyhanni.features.chroma

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.chroma.ChromaConfig.Direction
import at.hannibal2.skyhanni.data.MinecraftData
import at.hannibal2.skyhanni.mixins.transformers.AccessorMinecraft
import at.hannibal2.skyhanni.utils.shader.Shader
import at.hannibal2.skyhanni.utils.shader.Uniform
import net.minecraft.client.Minecraft

/**
 * Modified from SkyblockAddons
 *
 * Credit: [ChromaShader.java](https://github.com/BiscuitDevelopment/SkyblockAddons/blob/main/src/main/java/codes/biscuit/skyblockaddons/shader/chroma/ChromaShader.java)
 */

<<<<<<< HEAD
abstract class ChromaShader(vertex: String, fragment: String) : Shader(vertex, fragment) {
=======
object ChromaShader : Shader("chroma", "chroma") {

>>>>>>> 1af3c89a8daf1eac3921c8b26a49e379f0d18f08
    val config get() = SkyHanniMod.feature.chroma

    override fun registerUniforms() {
        registerUniform(Uniform.UniformType.FLOAT, "chromaSize") {
            config.chromaSize * (Minecraft.getMinecraft().displayWidth / 100f)
        }
        registerUniform(Uniform.UniformType.FLOAT, "timeOffset") {
            var ticks =
                (MinecraftData.totalTicks / 2) + (Minecraft.getMinecraft() as AccessorMinecraft).timer.renderPartialTicks

            ticks = when (config.chromaDirection) {
                Direction.FORWARD_RIGHT, Direction.BACKWARD_RIGHT -> ticks
                Direction.FORWARD_LEFT, Direction.BACKWARD_LEFT -> -ticks
                else -> ticks
            }

            val chromaSpeed = config.chromaSpeed / 360f
            ticks * chromaSpeed
        }
        registerUniform(Uniform.UniformType.FLOAT, "saturation") {
            config.chromaSaturation
        }
        registerUniform(Uniform.UniformType.BOOL, "forwardDirection") {
            when (config.chromaDirection) {
                Direction.FORWARD_RIGHT, Direction.FORWARD_LEFT -> true
                Direction.BACKWARD_RIGHT, Direction.BACKWARD_LEFT -> false
                else -> true
            }
        }
    }
}
