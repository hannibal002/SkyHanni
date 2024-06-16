package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.NEURenderEvent
import at.hannibal2.skyhanni.events.minecraft.ClientDisconnectEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import io.github.moulberry.notenoughupdates.NEUApi
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraftforge.client.event.GuiOpenEvent
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard

@SkyHanniModule
object GuiData {

    var preDrawEventCancelled = false

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onNeuRenderEvent(event: NEURenderEvent) {
        if (preDrawEventCancelled) event.cancel()
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onClick(event: GuiContainerEvent.SlotClickEvent) {
        if (preDrawEventCancelled) event.cancel()
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onGuiClick(event: GuiScreenEvent.MouseInputEvent.Pre) {
        if (preDrawEventCancelled) event.isCanceled = true
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onGuiKeyPress(event: GuiScreenEvent.KeyboardInputEvent.Pre) {
        val (escKey, invKey) = Minecraft.getMinecraft().gameSettings.let {
            Keyboard.KEY_ESCAPE to it.keyBindInventory.keyCode
        }
        if (escKey.isKeyHeld() || invKey.isKeyHeld()) return
        if (preDrawEventCancelled) event.isCanceled = true
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        DelayedRun.runNextTick {
            if (Minecraft.getMinecraft().currentScreen !is GuiChest) {
                preDrawEventCancelled = false
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        preDrawEventCancelled = false
    }

    @HandleEvent
    fun onDisconnect(event: ClientDisconnectEvent) {
        preDrawEventCancelled = false
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onGuiOpen(event: GuiOpenEvent) {
        if (preDrawEventCancelled) {
            NEUApi.setInventoryButtonsToDisabled()
        }
    }
}
