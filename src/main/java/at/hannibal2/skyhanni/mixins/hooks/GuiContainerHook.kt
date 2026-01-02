package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.data.GlobalRender
import at.hannibal2.skyhanni.data.GuiData
import at.hannibal2.skyhanni.events.DrawScreenAfterEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent.ClickType
import at.hannibal2.skyhanni.events.GuiContainerEvent.CloseWindowEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent.SlotClickEvent
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.SkyHanniGuiContainer
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

class GuiContainerHook(guiAny: Any) {

    private val gui: SkyHanniGuiContainer = guiAny as SkyHanniGuiContainer
    private val container: AbstractContainerMenu get() = gui.menu

    fun closeWindowPressed(ci: org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean>) {
        if (CloseWindowEvent(gui, container).post()) ci.cancel()
    }

    fun backgroundDrawn(context: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        if (GlobalRender.renderDisabled) return
        GuiContainerEvent.BackgroundDrawnEvent(context, gui, container, mouseX, mouseY, partialTicks).post()
    }

    fun preDraw(
        context: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        partialTicks: Float,
        ci: CallbackInfo,
    ) {
        if (GlobalRender.renderDisabled) return
        if (GuiContainerEvent.PreDraw(context, gui, container, mouseX, mouseY, partialTicks).post()) {
            GuiData.preDrawEventCancelled = true
            ci.cancel()
        } else {
            DelayedRun.runNextTick {
                GuiData.preDrawEventCancelled = false
            }
        }
    }

    fun postDraw(context: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        if (GlobalRender.renderDisabled) return
        GuiContainerEvent.PostDraw(context, gui, container, mouseX, mouseY, partialTicks).post()
    }

    fun foregroundDrawn(context: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        DrawContextUtils.setContext(context)
        DrawContextUtils.translate(0.0, 0.0, 200.0)

        GuiContainerEvent.ForegroundDrawnEvent(context, gui, container, mouseX, mouseY, partialTicks).post()
        DrawContextUtils.translate(0.0, 0.0, -200.0)
        DrawContextUtils.clearContext()
    }

    fun onDrawSlot(slot: Slot, ci: CallbackInfo) {
        val event = GuiContainerEvent.DrawSlotEvent.GuiContainerDrawSlotPre(gui, container, slot)
        if (event.post()) ci.cancel()
    }

    fun onDrawSlotPost(slot: Slot) {
        GuiContainerEvent.DrawSlotEvent.GuiContainerDrawSlotPost(gui, container, slot).post()
    }

    fun onMouseClick(slot: Slot?, slotId: Int, clickedButton: Int, clickType: Int, ci: CallbackInfo) {
        val item = container.items?.takeIf { it.size > slotId && slotId >= 0 }?.get(slotId)
        if (SlotClickEvent(gui, container, item, slot, slotId, clickedButton, ClickType.getTypeById(clickType)).post()
        ) ci.cancel()
    }

    fun onDrawScreenAfter(
        context: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        ci: CallbackInfo,
    ) {
        if (DrawScreenAfterEvent(context, mouseX, mouseY, ci).post()) ci.cancel()
    }

}
