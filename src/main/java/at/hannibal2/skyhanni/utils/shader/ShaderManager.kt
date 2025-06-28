package at.hannibal2.skyhanni.utils.shader

import at.hannibal2.skyhanni.features.chroma.StandardChromaShader
import at.hannibal2.skyhanni.features.chroma.TexturedChromaShader
import at.hannibal2.skyhanni.features.misc.DarkenShader
import at.hannibal2.skyhanni.shader.CircleShader
import at.hannibal2.skyhanni.shader.RoundedRectangleOutlineShader
import at.hannibal2.skyhanni.shader.RoundedRectangleShader
import at.hannibal2.skyhanni.shader.RoundedTextureShader
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.createResourceLocation
import net.minecraft.client.Minecraft
import org.apache.commons.lang3.StringUtils
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Object to handle shaders for SkyHanni
 */
object ShaderManager {

    /**
     * For any future shaders add the object instance in this enum and
     * in the when-expression
     */
    enum class Shaders(val shader: Shader) {
        STANDARD_CHROMA(StandardChromaShader.INSTANCE),
        TEXTURED_CHROMA(TexturedChromaShader.INSTANCE),
        ROUNDED_RECTANGLE(RoundedRectangleShader.INSTANCE),
        ROUNDED_RECT_OUTLINE(RoundedRectangleOutlineShader.INSTANCE),
        ROUNDED_TEXTURE(RoundedTextureShader.INSTANCE),
        DARKEN(DarkenShader.INSTANCE),
        CIRCLE(CircleShader.INSTANCE),
        ;

        fun enableShader() = enableShader(this)
    }

    private var activeShader: Shader? = null

    fun enableShader(shader: Shaders) {
        val shaderInstance = shader.shader

        if (!shaderInstance.created) return

        activeShader = shaderInstance
        shaderInstance.enable()
        shaderInstance.updateUniforms()
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
        val resourceLocation = createResourceLocation("skyhanni:shaders/$fileName${type.extension}")

        val source = StringBuilder()

        //#if MC < 1.21
        val inputStream = Minecraft.getMinecraft().resourceManager.getResource(resourceLocation).inputStream
        //#else
        //$$ val inputStream = MinecraftClient.getInstance().resourceManager.getResource(resourceLocation).get().inputStream
        //#endif
        BufferedReader(InputStreamReader(inputStream)).forEachLine {
            source.append(it).append("\n")
        }

        val shaderID = ShaderHelper.glCreateShader(type.shaderType)
        ShaderHelper.glShaderSource(shaderID, source.toString())
        ShaderHelper.glCompileShader(shaderID)

        if (ShaderHelper.glGetShaderInt(shaderID, ShaderHelper.GL_COMPILE_STATUS) == 0) {
            val errorMessage = "Failed to compile shader $fileName${type.extension}. Features that utilise this " +
                "shader will not work correctly, if at all"
            val errorLog = StringUtils.trim(ShaderHelper.glGetShaderInfoLog(shaderID, 1024))

            if (inWorld()) {
                ErrorManager.logErrorWithData(
                    Exception("Shader compilation error."),
                    errorMessage,
                    "GLSL Compilation Error:\n" to errorLog,
                )
            } else {
                ChatUtils.consoleLog("$errorMessage $errorLog")
            }

            return -1
        }

        return shaderID
    }

    fun inWorld() = MinecraftCompat.localWorldExists && MinecraftCompat.localPlayerExists
}

enum class ShaderType(val extension: String, val shaderType: Int) {
    VERTEX(".vsh", ShaderHelper.GL_VERTEX_SHADER),
    FRAGMENT(".fsh", ShaderHelper.GL_FRAGMENT_SHADER)
}
