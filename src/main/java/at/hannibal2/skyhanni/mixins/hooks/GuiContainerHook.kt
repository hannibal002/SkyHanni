package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.events.DrawScreenAfterEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent.CloseWindowEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent.SlotClickEvent
import at.hannibal2.skyhanni.test.SkyHanniDebugsAndTests
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.inventory.Slot
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

class GuiContainerHook(guiAny: Any) {

    val gui: GuiContainer

    init {
        gui = guiAny as GuiContainer
    }

    fun closeWindowPressed(ci: CallbackInfo) {
        if (CloseWindowEvent(gui, gui.inventorySlots).postAndCatch()) ci.cancel()
    }

    fun backgroundDrawn(mouseX: Int, mouseY: Int, partialTicks: Float) {
        if (!SkyHanniDebugsAndTests.globalRenderToggle) return
        GuiContainerEvent.BackgroundDrawnEvent(gui, gui.inventorySlots, mouseX, mouseY, partialTicks).postAndCatch()
    }

    fun foregroundDrawn(mouseX: Int, mouseY: Int, partialTicks: Float) {
        GuiContainerEvent.ForegroundDrawnEvent(gui, gui.inventorySlots, mouseX, mouseY, partialTicks).postAndCatch()
    }

    fun onDrawSlot(slot: Slot, ci: CallbackInfo) {
        val event = GuiContainerEvent.DrawSlotEvent.GuiContainerDrawSlotPre(gui, gui.inventorySlots, slot)
        if (event.postAndCatch()) ci.cancel()
    }

    fun onDrawSlotPost(slot: Slot) {
        GuiContainerEvent.DrawSlotEvent.GuiContainerDrawSlotPost(gui, gui.inventorySlots, slot).postAndCatch()
    }

    fun onMouseClick(slot: Slot?, slotId: Int, clickedButton: Int, clickType: Int, ci: CallbackInfo) {
        if (SlotClickEvent(gui, gui.inventorySlots, slot, slotId, clickedButton, clickType).postAndCatch()) ci.cancel()
    }

    fun onDrawScreenAfter(
        mouseX: Int,
        mouseY: Int,
        ci: CallbackInfo,
    ) {
        if (DrawScreenAfterEvent(mouseX, mouseY, ci).postAndCatch()) ci.cancel()
    }
}