package at.hannibal2.hanni.features.misc

import at.hannibal2.hanni.utils.shader.Shader
import at.hannibal2.hanni.utils.shader.Uniform

object DarkenShader : Shader("darken", "darken") {

    val INSTANCE: DarkenShader
        get() = this

    var darknessLevel = 0f

    override fun registerUniforms() {
        registerUniform(Uniform.UniformType.FLOAT, "darknessLevel") { darknessLevel }
    }
}

