package at.hannibal2.skyhanni.features.misc.customscoreboard

import at.hannibal2.skyhanni.utils.shader.Shader

object RoundedRectangleShader : Shader("rounded_rectangle", "rounded_rectangle") {

    val INSTANCE: RoundedRectangleShader
        get() = this

    override fun registerUniforms() {

    }
}
