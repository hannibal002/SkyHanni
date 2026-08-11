package at.hannibal2.skyhanni.config.core.elements

import org.lwjgl.glfw.GLFW


@Retention(RUNTIME)
@Target(FIELD)
annotation class ConfigEditorKeyMap(
    val defaultKey: Int = GLFW.GLFW_KEY_UNKNOWN,
    val displayName: String = "",
)
