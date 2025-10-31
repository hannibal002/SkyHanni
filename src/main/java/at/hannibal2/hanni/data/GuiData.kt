package at.hannibal2.hanni.data

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.GuiContainerEvent
import at.hannibal2.hanni.events.GuiKeyPressEvent
import at.hannibal2.hanni.events.InventoryCloseEvent
import at.hannibal2.hanni.events.NeuRenderEvent
import at.hannibal2.hanni.events.minecraft.ClientDisconnectEvent
import at.hannibal2.hanni.events.render.gui.GuiMouseInputEvent
import at.hannibal2.hanni.events.render.gui.GuiScreenOpenEvent
import at.hannibal2.hanni.features.inventory.wardrobe.CustomWardrobeKeybinds
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.DelayedRun
import at.hannibal2.hanni.utils.KeyboardManager.isActive
import at.hannibal2.hanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.hanni.utils.system.PlatformUtils
import io.github.moulberry.notenoughupdates.NEUApi
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import org.lwjgl.input.Keyboard

@HanniModule
object GuiData {

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
        if (CustomWardrobeKeybinds.allowMouseClick()) return

        if (preDrawEventCancelled) event.cancel()
    }

    @HandleEvent(priority = HandleEvent.HIGHEST)
    fun onGuiKeyPress(event: GuiKeyPressEvent) {
        val allowedKeys = with(Minecraft.getMinecraft().gameSettings) {
            listOf(
                keyBindInventory,
                keyBindScreenshot,
                keyBindFullscreen,
            )
        }
        if (allowedKeys.any { it.isActive() }) return
        if (Keyboard.KEY_ESCAPE.isKeyHeld()) return

        if (CustomWardrobeKeybinds.allowKeyboardClick()) return

        if (preDrawEventCancelled) event.cancel()
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        DelayedRun.runNextTick {
            if (Minecraft.getMinecraft().currentScreen !is GuiChest) {
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

    @HandleEvent(priority = HandleEvent.LOW)
    fun onGuiOpen(event: GuiScreenOpenEvent) {
        if (preDrawEventCancelled) {
            if (PlatformUtils.isNeuLoaded()) NEUApi.setInventoryButtonsToDisabled()
        }
    }
}
