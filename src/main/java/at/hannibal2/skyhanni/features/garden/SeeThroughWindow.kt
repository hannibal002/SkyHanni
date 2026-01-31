package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.minecraft.KeyDownEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils.afterChange
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW

@SkyHanniModule
object SeeThroughWindow {

    private val config get() = SkyHanniMod.feature.garden.seeThroughWindow

    private var isActive = false

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        config.seeThroughFarming.afterChange {
            setOpacity()
        }
    }

    @HandleEvent
    fun onKeyPressed(event: KeyDownEvent) {
        if (event.keyCode != config.keybind) return
        if (Minecraft.getInstance().screen != null) return
        isActive = !isActive
        setOpacity()
    }

    @HandleEvent
    fun onWorldSwap(event: WorldChangeEvent) {
        isActive = false
        setOpacity()
    }

    private fun setOpacity() {
        val handle = Minecraft.getInstance().window.handle()
        if (!isActive) {
            GLFW.glfwSetWindowOpacity(handle, 1f)
            return
        }
        val alpha = (config.seeThroughFarming.get() / 100f).coerceAtLeast(0.05f).coerceAtMost(1f)
        GLFW.glfwSetWindowOpacity(handle, alpha)
    }
}
