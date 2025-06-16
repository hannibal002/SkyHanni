package at.hannibal2.skyhanni.shader

import at.hannibal2.skyhanni.utils.shader.Shader
import at.hannibal2.skyhanni.utils.shader.Uniform
//#if MC < 1.21
import net.minecraft.client.Minecraft
//#else
//$$ import net.minecraft.client.MinecraftClient
//#endif

object CircleShader : Shader("circle", "circle") {

    val INSTANCE get() = this

    var scaleFactor: Float = 0f
    var radius: Float = 0f
    var smoothness: Float = 0f
    var centerPos: FloatArray = floatArrayOf(0f, 0f)
        set(value) {
            //#if MC < 1.21
            val gameHeight = Minecraft.getMinecraft().displayHeight
            //#else
            //$$ val gameHeight = MinecraftClient.getInstance().window.framebufferHeight.toFloat()
            //#endif
            field = floatArrayOf(value[0], gameHeight - value[1])
        }
    var angle1: Float = 0f
    var angle2: Float = 0f

    override fun registerUniforms() {
        registerUniform(Uniform.UniformType.FLOAT, "scaleFactor") { scaleFactor }
        registerUniform(Uniform.UniformType.FLOAT, "radius") { radius }
        registerUniform(Uniform.UniformType.FLOAT, "smoothness") { smoothness }
        registerUniform(Uniform.UniformType.FLOAT, "angle1") { angle1 }
        registerUniform(Uniform.UniformType.FLOAT, "angle2") { angle2 }
        registerUniform(Uniform.UniformType.VEC2, "centerPos") { centerPos }
    }
}
