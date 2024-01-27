package at.hannibal2.skyhanni.utils.shader

import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.resources.IReloadableResourceManager
import net.minecraft.client.shader.ShaderLinkHelper
import org.apache.commons.lang3.StringUtils
import org.lwjgl.opengl.GL11
import java.util.function.Supplier

/**
 * Superclass for shader objects to compile and attach vertex and fragment shaders to the shader program
 *
 * Modified class from SkyblockAddons
 *
 * Credit: [Shader.java](https://github.com/BiscuitDevelopment/SkyblockAddons/blob/main/src/main/java/codes/biscuit/skyblockaddons/shader/Shader.java)
 */
abstract class Shader(val vertex: String, val fragment: String) {

    var shaderProgram: Int = -1
    private var vertexShaderID: Int = -1
    private var fragmentShaderID: Int = -1

    private val uniforms: MutableList<Uniform<*>> = mutableListOf()

    init {
        recompile()
        (Minecraft.getMinecraft().resourceManager as IReloadableResourceManager).registerReloadListener {
            recompile()
        }
    }

    fun deleteOldShaders() {
        if (vertexShaderID >= 0) {
            OpenGlHelper.glDeleteShader(vertexShaderID)
            vertexShaderID = -1
        }
        if (fragmentShaderID >= 0) {
            OpenGlHelper.glDeleteShader(fragmentShaderID)
            fragmentShaderID = -1
        }
        if (shaderProgram >= 0) {
            OpenGlHelper.glDeleteProgram(shaderProgram);
        }
    }

    fun recompile() {
        deleteOldShaders()
        shaderProgram = ShaderLinkHelper.getStaticShaderLinkHelper().createProgram()
        vertexShaderID = ShaderManager.loadShader(ShaderType.VERTEX, vertex)
        ShaderManager.attachShader(shaderProgram, vertexShaderID)
        fragmentShaderID = ShaderManager.loadShader(ShaderType.FRAGMENT, fragment)
        ShaderManager.attachShader(shaderProgram, fragmentShaderID)

        ShaderHelper.glLinkProgram(shaderProgram)

        val linkStatus = ShaderHelper.glGetProgrami(shaderProgram, ShaderHelper.GL_LINK_STATUS)
        if (linkStatus == GL11.GL_FALSE) {
            LorenzUtils.consoleLog(
                "Error occurred when linking program with Vertex Shader: $vertex and Fragment Shader: $fragment : " +
                    StringUtils.trim(ShaderHelper.glGetProgramInfoLog(shaderProgram, 1024))
            )
        }

        this.registerUniforms()
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
