package at.hannibal2.skyhanni.utils.shader

import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.LorenzUtils
import java.util.function.Supplier
import net.minecraft.client.shader.ShaderLinkHelper
import org.apache.commons.lang3.StringUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.OpenGLException

/**
 * Superclass for shader objects to compile and attach vertex and fragment shaders to the shader program
 *
 * Modified class from SkyblockAddons
 *
 * Credit: [Shader.java](https://github.com/BiscuitDevelopment/SkyblockAddons/blob/main/src/main/java/codes/biscuit/skyblockaddons/shader/Shader.java)
 */
abstract class Shader(vertex: String, fragment: String) {

    var shaderProgram: Int = ShaderLinkHelper.getStaticShaderLinkHelper().createProgram()
    private val uniforms: MutableList<Uniform<*>> = mutableListOf()

    var created = false

    init {
        run {
            val vertexShaderID = ShaderManager.loadShader(ShaderType.VERTEX, vertex).also { if (it == -1) return@run }
            ShaderManager.attachShader(shaderProgram, vertexShaderID)

            val fragmentShaderID = ShaderManager.loadShader(ShaderType.FRAGMENT, fragment).also { if (it == -1) return@run }
            ShaderManager.attachShader(shaderProgram, fragmentShaderID)

            ShaderHelper.glLinkProgram(shaderProgram)

            if (ShaderHelper.glGetProgrami(shaderProgram, ShaderHelper.GL_LINK_STATUS) == GL11.GL_FALSE) {
                val errorMessage = "Failed to link vertex shader $vertex and fragment shader $fragment. Features that " +
                        "utilise this shader will not work correctly, if at all."
                val errorLog = StringUtils.trim(ShaderHelper.glGetShaderInfoLog(shaderProgram, 1024))

                if (ShaderManager.inWorld()) {
                    ErrorManager.logErrorWithData(
                            OpenGLException("Shader linking error."),
                            errorMessage,
                            "Link Error:\n" to errorLog
                    )
                } else {
                    LorenzUtils.consoleLog("$errorMessage $errorLog")
                }

                return@run
            }

            this.registerUniforms()
            created = true
        }
    }

    abstract fun registerUniforms()

    fun updateUniforms() {
        for (uniform in uniforms) {
            uniform.update()
        }
    }

    fun enable() = ShaderHelper.glUseProgram(shaderProgram)

    fun disable() = ShaderHelper.glUseProgram(0)

    fun <T> registerUniform(uniformType: Uniform.UniformType<T>, name: String, uniformValuesSupplier: Supplier<T>) {
        uniforms.add(Uniform(this, uniformType, name, uniformValuesSupplier))
    }
}
