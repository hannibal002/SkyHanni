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
    private var opacityChanged = false
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

        if (!isActive) {
            if (opacityChanged) {
                resetWindowOpacity()
            }
            return
        }
        val alpha = (config.seeThroughFarming.get() / 100f)
            .coerceAtLeast(0.05f)
            .coerceAtMost(1f)
        setWindowOpacity(alpha)
    }

    private fun setWindowOpacity(alpha: Float) {
        opacityChanged = alpha != 1f

        val handle = Minecraft.getInstance().window.handle()
        GLFW.glfwSetWindowOpacity(handle, alpha)
        val error = GLFW.glfwGetError(null)
        if (error.isGlfwPlatformError()) {
            unsupportedPlatform = true
            ChatUtils.userError("Your platform doesn't support see through windows!")
        }
    }

    private fun resetWindowOpacity() {
        setWindowOpacity(1f)
    }

    private fun Int.isGlfwPlatformError(): Boolean =
        this == GLFW.GLFW_PLATFORM_ERROR ||
            this == GLFW.GLFW_NOT_INITIALIZED ||
            this == GLFW.GLFW_FEATURE_UNAVAILABLE ||
            this == GLFW.GLFW_FEATURE_UNIMPLEMENTED
}
