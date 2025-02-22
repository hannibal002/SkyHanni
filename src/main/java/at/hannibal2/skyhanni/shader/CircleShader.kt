package at.hannibal2.skyhanni.shader

import at.hannibal2.skyhanni.utils.shader.Shader
import at.hannibal2.skyhanni.utils.shader.Uniform
import net.minecraft.client.Minecraft

object CircleShader : Shader("circle", "circle") {

    val INSTANCE get() = this

    var scaleFactor: Float = 0f
    var radius: Float = 0f
    var smoothness: Float = 0f
    var centerPos: FloatArray = floatArrayOf(0f, 0f)
        set(value) {
            field = floatArrayOf(value[0], Minecraft.getMinecraft().displayHeight - value[1])
        }

    override fun registerUniforms() {
        registerUniform(Uniform.UniformType.FLOAT, "scaleFactor") { scaleFactor }
        registerUniform(Uniform.UniformType.FLOAT, "radius") { radius }
        registerUniform(Uniform.UniformType.FLOAT, "smoothness") { smoothness }
        registerUniform(Uniform.UniformType.VEC2, "centerPos") { centerPos }
    }
}
