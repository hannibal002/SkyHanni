package at.hannibal2.skyhanni.features.chroma

import at.hannibal2.skyhanni.utils.shader.ShaderManager

/**
 * Object to handle enabling / disabling the chroma shader when rendering text
 *
 * Modified from SkyblockAddons
 *
 * Credit: [MulticolorShaderManager.java](https://github.com/BiscuitDevelopment/SkyblockAddons/blob/main/src/main/java/codes/biscuit/skyblockaddons/core/chroma/MulticolorShaderManager.java)
 */
object ChromaShaderManager {

    private var chromaEnabled = false

    fun begin() {
        disable()
        enable()
    }

    fun end() {
        disable()
    }

    private fun enable() {
        if (!chromaEnabled) {
            chromaEnabled = true
            ShaderManager.enableShader("chroma")
        }
    }

    private fun disable() {
        if (chromaEnabled) {
            chromaEnabled = false
            ShaderManager.disableShader()
        }
    }
}