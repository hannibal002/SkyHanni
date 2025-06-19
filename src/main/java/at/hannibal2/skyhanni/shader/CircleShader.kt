package at.hannibal2.skyhanni.shader

import at.hannibal2.skyhanni.features.misc.RoundedShader
import at.hannibal2.skyhanni.utils.shader.Uniform

object CircleShader : RoundedShader("circle", "circle") {

    val INSTANCE: CircleShader
        get() = this

    @Deprecated("Unused in this shader.")
    override var halfSize: FloatArray = floatArrayOf(0f, 0f)

    var angle1: Float = 0f
    var angle2: Float = 0f
    //#if MC > 1.21
    //$$ var modelViewMatrix: Matrix4f = Matrix4f()
    //#endif

    override fun registerUniforms() {
        registerUniform(Uniform.UniformType.FLOAT, "scaleFactor") { scaleFactor }
        registerUniform(Uniform.UniformType.FLOAT, "radius") { radius }
        registerUniform(Uniform.UniformType.FLOAT, "smoothness") { smoothness }
        registerUniform(Uniform.UniformType.FLOAT, "angle1") { angle1 }
        registerUniform(Uniform.UniformType.FLOAT, "angle2") { angle2 }
        registerUniform(Uniform.UniformType.VEC2, "centerPos") { centerPos }
    }
}
