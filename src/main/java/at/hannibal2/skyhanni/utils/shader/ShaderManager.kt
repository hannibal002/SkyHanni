package at.hannibal2.skyhanni.utils.shader

import at.hannibal2.skyhanni.features.chroma.ChromaShader
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import net.minecraft.util.ResourceLocation
import org.apache.commons.lang3.StringUtils
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Object to handle shaders for SkyHanni
 */
object ShaderManager {

    /**
     * For any future shaders add the object instance in this enum and
     * in the when expression
     */
    enum class Shaders(val shader: Shader) {
        CHROMA(ChromaShader.INSTANCE);

        companion object {
            fun getShaderInstance(shaderName: String): Shader? = when (shaderName) {
                "chroma" -> CHROMA.shader
                else -> {
                    null
                }
            }
        }
    }

    private val shaders: MutableMap<String, Shader> = mutableMapOf()
    private var activeShader: Shader? = null

    fun enableShader(shaderName: String) {
        var shader = shaders[shaderName]

        if (shader == null) {
            shader = Shaders.getShaderInstance(shaderName)
            if (shader == null) return
            shaders[shaderName] = shader
        }

        activeShader = shader
        shader.enable()
        shader.updateUniforms()
    }

    fun attachShader(shaderProgram: Int, shaderID: Int) {
        ShaderHelper.glAttachShader(shaderProgram, shaderID)
    }

    fun disableShader() {
        if (activeShader == null) return

        activeShader?.disable()
        activeShader = null
    }

    fun loadShader(type: ShaderType, fileName: String): Int {
        val resourceLocation = ResourceLocation("skyhanni:shaders/$fileName${type.extension}")

        val source = StringBuilder()

        val inputStream = Minecraft.getMinecraft().resourceManager.getResource(resourceLocation).inputStream
        BufferedReader(InputStreamReader(inputStream)).forEachLine {
            source.append(it).append("\n")
        }

        val shaderID = ShaderHelper.glCreateShader(type.shaderType)
        ShaderHelper.glShaderSource(shaderID, source.toString())
        ShaderHelper.glCompileShader(shaderID)

        if (ShaderHelper.glGetShaderi(shaderID, ShaderHelper.GL_COMPILE_STATUS) == 0) {
            LorenzUtils.consoleLog(
                "Error occurred when compiling shader $fileName${type.extension} : " +
                    StringUtils.trim(ShaderHelper.glGetShaderInfoLog(shaderID, 1024))
            )
        }

        return shaderID
    }
}

enum class ShaderType(val extension: String, val shaderType: Int) {
    VERTEX(".vsh", ShaderHelper.GL_VERTEX_SHADER),
    FRAGMENT(".fsh", ShaderHelper.GL_FRAGMENT_SHADER)
}
