package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiKeyPressEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.NeuRenderEvent
import at.hannibal2.skyhanni.events.minecraft.ClientDisconnectEvent
import at.hannibal2.skyhanni.events.render.gui.GuiMouseInputEvent
import at.hannibal2.skyhanni.features.inventory.loadout.CustomLoadoutKeybinds
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.KeyboardManager.isActive
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import org.lwjgl.glfw.GLFW

@SkyHanniModule
object GuiData {

    @JvmStatic
    var preDrawEventCancelled = false

    @HandleEvent(priority = HandleEvent.HIGH)
    fun onNeuRenderEvent(event: NeuRenderEvent) {
        if (preDrawEventCancelled) event.cancel()
    }

    @HandleEvent(priority = HandleEvent.HIGH)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (preDrawEventCancelled) event.cancel()
    }

    @HandleEvent
    fun onMouseInput(event: GuiMouseInputEvent) {
        if (CustomLoadoutKeybinds.allowMouseClick()) return

        if (preDrawEventCancelled) event.cancel()
    }

    @HandleEvent(priority = HandleEvent.HIGHEST)
    fun onGuiKeyPress(event: GuiKeyPressEvent) {
        val allowedKeys = with(Minecraft.getInstance().options) {
            listOf(
                keyInventory,
                keyScreenshot,
                keyFullscreen,
            )
        }
        if (allowedKeys.any { it.isActive() }) return
        if (GLFW.GLFW_KEY_ESCAPE.isKeyHeld()) return

        if (CustomLoadoutKeybinds.allowKeyboardClick()) return

        if (preDrawEventCancelled) event.cancel()
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        DelayedRun.runNextTick {
            if (MinecraftCompat.screen !is ContainerScreen) {
                preDrawEventCancelled = false
            }
        }
    }

    @HandleEvent
    fun onWorldChange() {
        preDrawEventCancelled = false
    }

    @HandleEvent
    fun onDisconnect(event: ClientDisconnectEvent) {
        preDrawEventCancelled = false
    }
}
