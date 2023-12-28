package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.utils.shader.Shader
import at.hannibal2.skyhanni.utils.shader.Uniform
import net.minecraft.client.Minecraft

object RoundedRectangleShader : Shader("rounded_rect", "rounded_rect") {

    val INSTANCE: RoundedRectangleShader
        get() = this

    var radius: Float = 0f
    var smoothness: Float = 0f
    var halfSize: FloatArray = floatArrayOf(0f, 0f)
    var centerPos: FloatArray = floatArrayOf(0f, 0f)
        set(value) {
            field = floatArrayOf(value[0], Minecraft.getMinecraft().displayHeight - value[1])
        }

    override fun registerUniforms() {
        registerUniform(Uniform.UniformType.FLOAT, "radius") { radius }
        registerUniform(Uniform.UniformType.FLOAT, "smoothness") { smoothness }
        registerUniform(Uniform.UniformType.VEC2, "halfSize") { halfSize }
        registerUniform(Uniform.UniformType.VEC2, "centerPos") { centerPos }
    }
}