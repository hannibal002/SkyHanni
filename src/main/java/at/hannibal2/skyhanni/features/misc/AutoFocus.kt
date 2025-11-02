package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import net.minecraft.client.MinecraftClient
import org.lwjgl.glfw.GLFW

// This has intentionally not any settings.
// If a dev does not want to have their game pop in foreground after 5 min of compilation,
// please add a config toggle that is default enabled.
@SkyHanniModule
object AutoFocus {

    var dirty = false

    @HandleEvent(SkyHanniTickEvent::class)
    fun onHypixelJoin() {
        if (dirty) return
        dirty = true

        if (PlatformUtils.isDevEnvironment) {
            val handle: Long = MinecraftClient.getInstance().window.handle
            GLFW.glfwFocusWindow(handle)
        }
    }
}
