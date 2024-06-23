package at.hannibal2.skyhanni.mixins.kotlin

import at.hannibal2.skyhanni.data.GuiData
import at.hannibal2.skyhanni.data.ToolTipData
import at.hannibal2.skyhanni.events.DrawScreenAfterEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent.CloseWindowEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent.SlotClickEvent
import at.hannibal2.skyhanni.kmixin.annotations.InjectionKind
import at.hannibal2.skyhanni.kmixin.annotations.KInject
import at.hannibal2.skyhanni.kmixin.annotations.KInjectAt
import at.hannibal2.skyhanni.kmixin.annotations.KMixin
import at.hannibal2.skyhanni.kmixin.annotations.KSelf
import at.hannibal2.skyhanni.kmixin.annotations.KShadow
import at.hannibal2.skyhanni.kmixin.annotations.TargetShift
import at.hannibal2.skyhanni.test.SkyHanniDebugsAndTests
import at.hannibal2.skyhanni.utils.DelayedRun
import io.github.moulberry.notenoughupdates.NEUApi
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.inventory.Slot
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@KMixin(GuiContainer::class)
object GuiContainerMixin {

    @KInjectAt(
        method = "keyTyped",
        target = "Lnet/minecraft/client/entity/EntityPlayerSP;closeScreen()V",
        shift = TargetShift.BEFORE,
        cancellable = true
    )
    fun closeWindowPressed(ci: CallbackInfo, @KSelf gui: GuiContainer) {
        if (CloseWindowEvent(gui, gui.inventorySlots).postAndCatch()) {
            ci.cancel()
        }
    }

    @KInjectAt(
        method = "drawScreen",
        target = "Lnet/minecraft/client/renderer/GlStateManager;color(FFFF)V",
        ordinal = 1
    )
    fun backgroundDrawn(mouseX: Int, mouseY: Int, partialTicks: Float, ci: CallbackInfo, @KSelf gui: GuiContainer) {
        if (!SkyHanniDebugsAndTests.globalRender) return
        GuiContainerEvent.BackgroundDrawnEvent(gui, gui.inventorySlots, mouseX, mouseY, partialTicks).postAndCatch()
    }

    @KInject(method = "drawScreen", kind = InjectionKind.HEAD, cancellable = true)
    fun preDraw(mouseX: Int, mouseY: Int, partialTicks: Float, ci: CallbackInfo, @KSelf gui: GuiContainer) {
        if (!SkyHanniDebugsAndTests.globalRender) return
        if (GuiContainerEvent.BeforeDraw(gui, gui.inventorySlots, mouseX, mouseY, partialTicks).postAndCatch()) {
            NEUApi.setInventoryButtonsToDisabled()
            GuiData.preDrawEventCancelled = true
            ci.cancel()
        } else {
            DelayedRun.runNextTick {
                GuiData.preDrawEventCancelled = false
            }
        }
    }

    @KInjectAt(
        method = "drawScreen",
        target = "Lnet/minecraft/client/gui/inventory/GuiContainer;drawGuiContainerForegroundLayer(II)V",
        shift = TargetShift.AFTER
    )
    fun onForegroundDraw(mouseX: Int, mouseY: Int, partialTicks: Float, ci: CallbackInfo, @KSelf gui: GuiContainer) {
        GuiContainerEvent.ForegroundDrawnEvent(gui, gui.inventorySlots, mouseX, mouseY, partialTicks).postAndCatch()
    }

    @KInject(method = "drawSlot", kind = InjectionKind.HEAD, cancellable = true)
    fun onDrawSlot(slot: Slot, ci: CallbackInfo, @KSelf gui: GuiContainer) {
        val event = GuiContainerEvent.DrawSlotEvent.GuiContainerDrawSlotPre(gui, gui.inventorySlots, slot)
        if (event.postAndCatch()) ci.cancel()
    }

    @KInject(method = "drawSlot", kind = InjectionKind.RETURN)
    fun onDrawSlotPost(slot: Slot, ci: CallbackInfo, @KSelf gui: GuiContainer) {
        GuiContainerEvent.DrawSlotEvent.GuiContainerDrawSlotPost(gui, gui.inventorySlots, slot).postAndCatch()
    }

    @KInjectAt(
        method = "handleMouseClick",
        target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;" +
            "windowClick(IIIILnet/minecraft/entity/player/EntityPlayer;)Lnet/minecraft/item/ItemStack;",
        cancellable = true
    )
    fun onMouseClick(
        slot: Slot?,
        slotId: Int,
        clickedButton: Int,
        clickType: Int,
        ci: CallbackInfo,
        @KSelf gui: GuiContainer
    ) {
        val item = gui.inventorySlots?.inventory?.takeIf { it.size > slotId && slotId >= 0 }?.get(slotId)
        if (SlotClickEvent(gui, gui.inventorySlots, item, slot, slotId, clickedButton, clickType).postAndCatch()) {
            ci.cancel()
        }
    }

    @KInjectAt(
        method = "drawScreen",
        target = "Lnet/minecraft/entity/player/InventoryPlayer;getItemStack()Lnet/minecraft/item/ItemStack;",
        shift = TargetShift.BEFORE,
        ordinal = 1
    )
    fun drawScreenPost(mouseX: Int, mouseY: Int, partialTicks: Float, ci: CallbackInfo, @KShadow theSlot: Slot?) {
        if (DrawScreenAfterEvent(mouseX, mouseY, ci).postAndCatch()) ci.cancel()
        ToolTipData.lastSlot = theSlot
    }

}
