package at.hannibal2.skyhanni.shader

import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import at.hannibal2.skyhanni.utils.shader.Shader
import at.hannibal2.skyhanni.utils.shader.Uniform
//#if MC > 1.21
//$$ import org.joml.Matrix4f
//#endif

abstract class RoundedShader<Self : RoundedShader<Self>>(vertex: String, fragment: String) : Shader(vertex, fragment) {
    @Suppress("UNCHECKED_CAST", "PropertyName", "VariableNaming")
    val INSTANCE: Self
        get() = this as Self
    var scaleFactor: Float = 0f
    var radius: Float = 0f
    var smoothness: Float = 0f
    open var halfSize: FloatArray = floatArrayOf(0f, 0f)
    var centerPos: FloatArray = floatArrayOf(0f, 0f)
        set(value) {
            field = floatArrayOf(value[0], GuiScreenUtils.displayHeight - value[1])
        }
    //#if MC > 1.21
    //$$ var modelViewMatrix: Matrix4f = Matrix4f()
    //#endif

    fun applyBaseUniforms(hasSmoothness: Boolean = true) {
        registerUniform(Uniform.UniformType.FLOAT, "scaleFactor") { scaleFactor }
        registerUniform(Uniform.UniformType.FLOAT, "radius") { radius }
        if (hasSmoothness) registerUniform(Uniform.UniformType.FLOAT, "smoothness") { smoothness }
        registerUniform(Uniform.UniformType.VEC2, "halfSize") { halfSize }
        registerUniform(Uniform.UniformType.VEC2, "centerPos") { centerPos }
    }

    override fun registerUniforms() = applyBaseUniforms()
}

object RoundedRectangleShader : RoundedShader<RoundedRectangleShader>("rounded_rect", "rounded_rect")
object RoundedTextureShader : RoundedShader<RoundedTextureShader>("rounded_texture", "rounded_texture")
object RoundedRectangleOutlineShader : RoundedShader<RoundedRectangleOutlineShader>(
    "rounded_rect_outline",
    "rounded_rect_outline"
) {
    var borderThickness: Float = 5f
    var borderBlur: Float = 0.3f

    override fun registerUniforms() {
        super.applyBaseUniforms(hasSmoothness = false)
        registerUniform(Uniform.UniformType.FLOAT, "borderThickness") { borderThickness }
        registerUniform(Uniform.UniformType.FLOAT, "borderBlur") { borderBlur }
    }
}
