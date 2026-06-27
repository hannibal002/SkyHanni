package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.compat.SkyHanniGuiContainer
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.Slot

abstract class GuiContainerEvent(open val gui: SkyHanniGuiContainer, open val container: AbstractContainerMenu) : SkyHanniEvent() {

    @PrimaryFunction("onBackgroundDrawn")
    data class BackgroundDrawnEvent(
        override val context: GuiGraphicsExtractor,
        override val gui: SkyHanniGuiContainer,
        override val container: AbstractContainerMenu,
        val mouseX: Int,
        val mouseY: Int,
        val partialTicks: Float,
    ) : GuiContainerEvent(gui, container), Rendering

    data class PreDraw(
        override val context: GuiGraphicsExtractor,
        override val gui: SkyHanniGuiContainer,
        override val container: AbstractContainerMenu,
        val mouseX: Int,
        val mouseY: Int,
        val partialTicks: Float,
    ) : GuiContainerEvent(gui, container), Cancellable, Rendering

    data class PostDraw(
        override val context: GuiGraphicsExtractor,
        override val gui: SkyHanniGuiContainer,
        override val container: AbstractContainerMenu,
        val mouseX: Int,
        val mouseY: Int,
        val partialTicks: Float,
    ) : GuiContainerEvent(gui, container), Rendering

    data class CloseWindowEvent(override val gui: SkyHanniGuiContainer, override val container: AbstractContainerMenu) :
        GuiContainerEvent(gui, container), Cancellable

    abstract class DrawSlotEvent(gui: SkyHanniGuiContainer, container: AbstractContainerMenu, open val slot: Slot) :
        GuiContainerEvent(gui, container) {

        data class GuiContainerDrawSlotPre(
            override val gui: SkyHanniGuiContainer,
            override val container: AbstractContainerMenu,
            override val slot: Slot,
        ) :
            DrawSlotEvent(gui, container, slot), Cancellable

        data class GuiContainerDrawSlotPost(
            override val gui: SkyHanniGuiContainer,
            override val container: AbstractContainerMenu,
            override val slot: Slot,
        ) :
            DrawSlotEvent(gui, container, slot)
    }

    data class ForegroundDrawnEvent(
        override val context: GuiGraphicsExtractor,
        override val gui: SkyHanniGuiContainer,
        override val container: AbstractContainerMenu,
        val mouseX: Int,
        val mouseY: Int,
        val partialTicks: Float,
    ) : GuiContainerEvent(gui, container), Rendering

    data class SlotClickEvent(
        override val gui: SkyHanniGuiContainer,
        override val container: AbstractContainerMenu,
        val item: SafeItemStack?,
        val slot: Slot?,
        val slotId: Int,
        val clickedButton: Int,
        val clickType: ContainerInput?,
    ) : GuiContainerEvent(gui, container), Cancellable {

        fun makePickblock() {
            if (this.clickedButton == 2 && this.clickType == ContainerInput.CLONE) return
            slot?.index?.let { slotNumber ->
                InventoryUtils.clickSlot(slotNumber, container.containerId, mouseButton = 2, mode = ContainerInput.CLONE)
                cancel()
            }
        }
    }

}
