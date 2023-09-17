package at.hannibal2.skyhanni.utils.shader

import java.util.Objects
import java.util.function.Supplier

/**
 * Class to handle shader uniform types
 *
 * Modified from SkyblockAddons
 *
 * Credit: [Uniform.java](https://github.com/BiscuitDevelopment/SkyblockAddons/blob/main/src/main/java/codes/biscuit/skyblockaddons/shader/Uniform.java)
 */
class Uniform<T>(
    shader: Shader,
    private val uniformType: UniformType<T>,
    val name: String,
    private val uniformValuesSupplier: Supplier<T>
) {

    class UniformType<T> {
        companion object {
            val FLOAT: UniformType<Float> = UniformType()
            val VEC3: UniformType<FloatArray> = UniformType()
        }
    }

    private val uniformID: Int = ShaderHelper.glGetUniformLocation(shader.shaderProgram, name)
    private var previousUniformValue: T? = null

    fun update() {
        val newUniformValue: T = uniformValuesSupplier.get()
        if (!Objects.deepEquals(previousUniformValue, newUniformValue)) {
            if (uniformType == UniformType.FLOAT) {
                ShaderHelper.glUniform1f(uniformID, (newUniformValue as Float))
            } else if (uniformType == UniformType.VEC3) {
                val values = newUniformValue as FloatArray
                ShaderHelper.glUniform3f(uniformID, values[0], values[1], values[2])
            }
            previousUniformValue = newUniformValue
        }
    }
}