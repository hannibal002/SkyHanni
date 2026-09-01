package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.data.GlobalRender
import at.hannibal2.skyhanni.data.GuiData
import at.hannibal2.skyhanni.events.DrawScreenAfterEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent.CloseWindowEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent.SlotClickEvent
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.SkyHanniGuiContainer
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.Slot
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

class GuiContainerHook(guiAny: Any) {
    private val gui: SkyHanniGuiContainer = guiAny as SkyHanniGuiContainer
    private val container: AbstractContainerMenu get() = gui.menu

    fun closeWindowPressed(ci: CallbackInfoReturnable<Boolean>) {
        if (CloseWindowEvent(gui, container).post().isCancelled) ci.cancel()
    }

    fun backgroundDrawn(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTicks: Float) {
        if (GlobalRender.renderDisabled) return
        GuiContainerEvent.BackgroundDrawnEvent(context, gui, container, mouseX, mouseY, partialTicks).post()
    }

    fun preDraw(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        partialTicks: Float,
        ci: CallbackInfo,
    ) {
        if (GlobalRender.renderDisabled) return
        if (GuiContainerEvent.PreDraw(context, gui, container, mouseX, mouseY, partialTicks).post().isCancelled) {
            GuiData.preDrawEventCancelled = true
            ci.cancel()
        } else {
            DelayedRun.runNextTick {
                GuiData.preDrawEventCancelled = false
            }
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

    fun onDrawSlot(slot: Slot, ci: CallbackInfo) {
        val event = GuiContainerEvent.DrawSlotEvent.GuiContainerDrawSlotPre(gui, container, slot)
        if (event.post().isCancelled) ci.cancel()
    }

    fun onDrawSlotPost(slot: Slot) {
        GuiContainerEvent.DrawSlotEvent.GuiContainerDrawSlotPost(gui, container, slot).post()
    }

    fun onMouseClick(
        // Required for Java interop with Operation<Void>
        @Suppress("ForbiddenVoid")
        original: Operation<Void>,
        slot: Slot?,
        slotId: Int,
        buttonNum: Int,
        containerInput: ContainerInput,
    ) {
        val event = SlotClickEvent.postEvent(gui, container, slotId, buttonNum, containerInput)

        if (event == null) {
            original.call(slot, slotId, buttonNum, containerInput)
            return
        }

        if (event.isCancelled) return

        original.call(
            event.slot,
            event.slotId,
            event.clickedButton,
            event.clickType,
        )
    }

    fun onDrawScreenAfter(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        ci: CallbackInfo,
    ) {
        if (DrawScreenAfterEvent(context, mouseX, mouseY, ci).post().isCancelled) ci.cancel()
    }
}
