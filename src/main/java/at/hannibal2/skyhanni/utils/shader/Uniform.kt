package at.hannibal2.skyhanni.utils.shader

import java.util.Objects
//#if MC > 1.21
//$$ import org.joml.Matrix4f
//#endif

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
    private val uniformValuesSupplier: () -> T,
) {

    @Suppress("UtilityClassWithPublicConstructor")
    class UniformType<T> {
        companion object {

            val FLOAT: UniformType<Float> = UniformType()
            val VEC2: UniformType<FloatArray> = UniformType()
            val VEC3: UniformType<FloatArray> = UniformType()
            val VEC4: UniformType<FloatArray> = UniformType()
            val BOOL: UniformType<Boolean> = UniformType()
            val INT: UniformType<Int> = UniformType()
            //#if MC > 1.21
            //$$ val MAT4: UniformType<Matrix4f> = UniformType()
            //#endif
        }
    }

    private val uniformID: Int = ShaderHelper.glGetUniformLocation(shader.shaderProgram, name)
    private var previousUniformValue: T? = null

    fun update() {
        val newUniformValue: T = uniformValuesSupplier()
        if (!Objects.deepEquals(previousUniformValue, newUniformValue)) {
            when (uniformType) {
                UniformType.FLOAT -> {
                    ShaderHelper.glUniform1f(uniformID, (newUniformValue as Float))
                }

                UniformType.VEC2 -> {
                    val values = newUniformValue as FloatArray
                    ShaderHelper.glUniform2f(uniformID, values[0], values[1])
                }

                UniformType.VEC3 -> {
                    val values = newUniformValue as FloatArray
                    ShaderHelper.glUniform3f(uniformID, values[0], values[1], values[2])
                }

                UniformType.VEC4 -> {
                    val values = newUniformValue as FloatArray
                    ShaderHelper.glUniform4f(uniformID, values[0], values[1], values[2], values[3])
                }

                //#if MC > 1.21
                //$$ UniformType.MAT4 -> {
                //$$     val matrix = newUniformValue as Matrix4f
                //$$     ShaderHelper.glUniformMatrix4f(uniformID, false, matrix)
                //$$ }
                //#endif

                UniformType.BOOL -> ShaderHelper.glUniform1f(uniformID, if (newUniformValue as Boolean) 1f else 0f)
                UniformType.INT -> ShaderHelper.glUniform1i(uniformID, (newUniformValue as Int))
            }
            previousUniformValue = newUniformValue
        }
    }
}
