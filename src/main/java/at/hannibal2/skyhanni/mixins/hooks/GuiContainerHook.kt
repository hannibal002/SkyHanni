package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.data.GlobalRender
import at.hannibal2.skyhanni.data.GuiData
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent.CloseWindowEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent.SlotClickEvent
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.SkyHanniGuiContainer
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.Slot

class GuiContainerHook(guiAny: Any) {

    private val gui: SkyHanniGuiContainer = guiAny as SkyHanniGuiContainer
    private val container: AbstractContainerMenu get() = gui.menu

    fun shouldCancelCloseWindow(): Boolean = CloseWindowEvent(gui, container).post()

    fun backgroundDrawn(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTicks: Float) {
        if (GlobalRender.renderDisabled) return
        GuiContainerEvent.BackgroundDrawnEvent(context, gui, container, mouseX, mouseY, partialTicks).post()
    }

    fun shouldCancelPreDraw(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        partialTicks: Float,
    ): Boolean {
        if (GlobalRender.renderDisabled) return false
        return if (GuiContainerEvent.PreDraw(context, gui, container, mouseX, mouseY, partialTicks).post()) {
            GuiData.preDrawEventCancelled = true
            true
        } else {
            DelayedRun.runNextTick {
                GuiData.preDrawEventCancelled = false
            }
            false
        }
    }

    fun postDraw(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTicks: Float) {
        if (GlobalRender.renderDisabled) return
        GuiContainerEvent.PostDraw(context, gui, container, mouseX, mouseY, partialTicks).post()
    }

    fun foregroundDrawn(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTicks: Float) {
        DrawContextUtils.setContext(context)
        DrawContextUtils.translate(0.0, 0.0)

        GuiContainerEvent.ForegroundDrawnEvent(context, gui, container, mouseX, mouseY, partialTicks).post()
        DrawContextUtils.translate(0.0, 0.0)
        DrawContextUtils.clearContext()
    }

    fun shouldCancelDrawSlot(slot: Slot) =
        GuiContainerEvent.DrawSlotEvent.GuiContainerDrawSlotPre(gui, container, slot).post()

    fun onDrawSlotPost(slot: Slot) {
        GuiContainerEvent.DrawSlotEvent.GuiContainerDrawSlotPost(gui, container, slot).post()
    }

    fun shouldCancelMouseClick(slot: Slot?, slotId: Int, clickedButton: Int, clickType: ContainerInput): Boolean {
        val item = container.items?.takeIf { it.size > slotId && slotId >= 0 }?.get(slotId)
        return SlotClickEvent(gui, container, item, slot, slotId, clickedButton, clickType).post()
    }
}
