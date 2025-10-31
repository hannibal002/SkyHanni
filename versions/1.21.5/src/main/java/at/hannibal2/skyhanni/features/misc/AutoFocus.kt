package at.hannibal2.hanni.features.misc

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.minecraft.HanniTickEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.system.PlatformUtils
import net.minecraft.client.MinecraftClient
import org.lwjgl.glfw.GLFW

// This has intentionally not any settings.
// If a dev does not want to have their game pop in foreground after 5 min of compilation,
// please add a config toggle that is default enabled.
@HanniModule
object AutoFocus {

    var dirty = false

    @HandleEvent(HanniTickEvent::class)
    fun onHypixelJoin() {
        if (dirty) return
        dirty = true

        if (PlatformUtils.isDevEnvironment) {
            val handle: Long = MinecraftClient.getInstance().window.handle
            GLFW.glfwFocusWindow(handle)
        }
    }
}
