package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils.afterChange
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW

@SkyHanniModule
object SeeThroughWindow {
    private val config get() = SkyHanniMod.feature.garden.seeThroughWindow

    private var isActive = false
    private var currentOpacity = 1f
    private var unsupportedPlatform = false

    @HandleEvent
    private fun onConfigLoad() {
        config.seeThroughFarming.afterChange {
            setOpacity()
        }
    }

    @HandleEvent
    private fun onKeyDown() {
        if (!config.keybind.isKeyHeld()) return
        if (MinecraftCompat.screen != null) return

        isActive = !isActive
        setOpacity()
    }

    @HandleEvent
    private fun onWorldChange() {
        isActive = false
        setOpacity()
    }

    private fun setOpacity() {
        if (unsupportedPlatform) return

        val targetOpacity = if (isActive) {
            (config.seeThroughFarming.get() / 100f)
                .coerceIn(0.05f, 1f)
        } else {
            1f
        }

        if (currentOpacity == targetOpacity) return
        if (setWindowOpacity(targetOpacity)) {
            currentOpacity = targetOpacity
        }
    }

    private fun setWindowOpacity(alpha: Float): Boolean {
        val handle = Minecraft.getInstance().window.handle()

        GLFW.glfwSetWindowOpacity(handle, alpha)

        val error = GLFW.glfwGetError(null)
        if (error.isGlfwPlatformError()) {
            unsupportedPlatform = true
            ChatUtils.userError("Your platform doesn't support see through windows!")
            return false
        }
        return true
    }

    private fun Int.isGlfwPlatformError(): Boolean =
        when (this) {
            GLFW.GLFW_PLATFORM_ERROR,
            GLFW.GLFW_PLATFORM_UNAVAILABLE,
            GLFW.GLFW_API_UNAVAILABLE,
            GLFW.GLFW_NOT_INITIALIZED,
            GLFW.GLFW_FEATURE_UNAVAILABLE,
            GLFW.GLFW_FEATURE_UNIMPLEMENTED -> true
            else -> false
        }
}
