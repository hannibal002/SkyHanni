package at.hannibal2.hanni.features.chroma

import at.hannibal2.hanni.api.minecraftevents.ClientEvents
import at.hannibal2.hanni.config.features.chroma.ChromaConfig.Direction
import at.hannibal2.hanni.mixins.transformers.AccessorMinecraft
import at.hannibal2.hanni.utils.compat.GuiScreenUtils
import at.hannibal2.hanni.utils.shader.Shader
import at.hannibal2.hanni.utils.shader.Uniform
import net.minecraft.client.Minecraft

/**
 * Modified from SkyblockAddons
 *
 * Credit: [ChromaShader.java](https://github.com/BiscuitDevelopment/SkyblockAddons/blob/main/src/main/java/codes/biscuit/skyblockaddons/shader/chroma/ChromaShader.java)
 */

abstract class ChromaShader(vertex: String, fragment: String) : Shader(vertex, fragment) {

    override fun registerUniforms() {
        registerUniform(Uniform.UniformType.FLOAT, "chromaSize") {
            ChromaManager.config.chromaSize * (GuiScreenUtils.displayWidth / 100f)
        }
        registerUniform(Uniform.UniformType.FLOAT, "timeOffset") {
            //#if MC < 1.21
            var ticks = (ClientEvents.totalTicks) + (Minecraft.getMinecraft() as AccessorMinecraft).timer.renderPartialTicks
            //#else
            //$$ var ticks = (ClientEvents.totalTicks) + (MinecraftClient.getInstance() as AccessorMinecraft).timer.getTickProgress(true)
            //#endif

            ticks = when (ChromaManager.config.chromaDirection) {
                Direction.FORWARD_RIGHT, Direction.BACKWARD_RIGHT -> ticks
                Direction.FORWARD_LEFT, Direction.BACKWARD_LEFT -> -ticks
            }

            val chromaSpeed = ChromaManager.config.chromaSpeed / 360f
            ticks * chromaSpeed
        }
        registerUniform(Uniform.UniformType.FLOAT, "saturation") {
            ChromaManager.config.chromaSaturation
        }
        registerUniform(Uniform.UniformType.BOOL, "forwardDirection") {
            when (ChromaManager.config.chromaDirection) {
                Direction.FORWARD_RIGHT, Direction.FORWARD_LEFT -> true
                Direction.BACKWARD_RIGHT, Direction.BACKWARD_LEFT -> false
            }
        }
    }
}
