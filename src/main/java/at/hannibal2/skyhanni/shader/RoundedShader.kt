package at.hannibal2.skyhanni.shader

import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import org.joml.Matrix4f

abstract class RoundedShader<Self : RoundedShader<Self>> {
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

    var modelViewMatrix: Matrix4f = Matrix4f()
}

object RoundedRectangleShader : RoundedShader<RoundedRectangleShader>()
object RoundedTextureShader : RoundedShader<RoundedTextureShader>()
object RoundedRectangleOutlineShader : RoundedShader<RoundedRectangleOutlineShader>() {
    var borderThickness: Float = 5f
    var borderBlur: Float = 0.3f
}

object CircleShader : RoundedShader<CircleShader>() {
    var angle1: Float = 0f
    var angle2: Float = 0f
}

object RadialGradientCircleShader : RoundedShader<RadialGradientCircleShader>() {
    var angle: Float = 0f
    var startColor: FloatArray = floatArrayOf(0f, 0f, 0f, 0f)
    var endColor: FloatArray = floatArrayOf(0f, 0f, 0f, 0f)
    var progress: Float = 0f
    var phaseOffset: Float = 0f
    var reverse: Int = 0
}
