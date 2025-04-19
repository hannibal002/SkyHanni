package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.data.GuiData
import at.hannibal2.skyhanni.events.DrawScreenAfterEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent.ClickType
import at.hannibal2.skyhanni.events.GuiContainerEvent.CloseWindowEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent.SlotClickEvent
import at.hannibal2.skyhanni.test.SkyHanniDebugsAndTests
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.compat.DrawContext
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import io.github.moulberry.notenoughupdates.NEUApi
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.inventory.Slot
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

class GuiContainerHook(guiAny: Any) {

    val gui: GuiContainer = guiAny as GuiContainer

    fun closeWindowPressed(ci: CallbackInfo) {
        if (CloseWindowEvent(gui, gui.inventorySlots).post()) ci.cancel()
    }

    fun backgroundDrawn(context: DrawContext, mouseX: Int, mouseY: Int, partialTicks: Float) {
        if (!SkyHanniDebugsAndTests.globalRender) return
        GuiContainerEvent.BackgroundDrawnEvent(context, gui, gui.inventorySlots, mouseX, mouseY, partialTicks).post()
    }

    fun preDraw(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int,
        partialTicks: Float,
        ci: CallbackInfo,
    ) {
        if (!SkyHanniDebugsAndTests.globalRender) return
        if (GuiContainerEvent.PreDraw(context, gui, gui.inventorySlots, mouseX, mouseY, partialTicks).post()) {
            if (PlatformUtils.isNeuLoaded()) NEUApi.setInventoryButtonsToDisabled()
            GuiData.preDrawEventCancelled = true
            ci.cancel()
        } else {
            DelayedRun.runNextTick {
                GuiData.preDrawEventCancelled = false
            }
        }
    }

    fun postDraw(context: DrawContext, mouseX: Int, mouseY: Int, partialTicks: Float) {
        if (!SkyHanniDebugsAndTests.globalRender) return
        GuiContainerEvent.PostDraw(context, gui, gui.inventorySlots, mouseX, mouseY, partialTicks).post()
    }

    fun foregroundDrawn(context: DrawContext, mouseX: Int, mouseY: Int, partialTicks: Float) {
        GuiContainerEvent.ForegroundDrawnEvent(context, gui, gui.inventorySlots, mouseX, mouseY, partialTicks).post()
    }

    fun onDrawSlot(slot: Slot, ci: CallbackInfo) {
        val event = GuiContainerEvent.DrawSlotEvent.GuiContainerDrawSlotPre(gui, gui.inventorySlots, slot)
        if (event.post()) ci.cancel()
    }

    fun onDrawSlotPost(slot: Slot) {
        GuiContainerEvent.DrawSlotEvent.GuiContainerDrawSlotPost(gui, gui.inventorySlots, slot).post()
    }

    fun onMouseClick(slot: Slot?, slotId: Int, clickedButton: Int, clickType: Int, ci: CallbackInfo) {
        val item = gui.inventorySlots?.inventory?.takeIf { it.size > slotId && slotId >= 0 }?.get(slotId)
        if (SlotClickEvent(gui, gui.inventorySlots, item, slot, slotId, clickedButton, ClickType.getTypeById(clickType)).post()
        ) ci.cancel()
    }

    fun onDrawScreenAfter(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int,
        ci: CallbackInfo,
    ) {
        if (DrawScreenAfterEvent(context, mouseX, mouseY, ci).post()) ci.cancel()
    }

}
