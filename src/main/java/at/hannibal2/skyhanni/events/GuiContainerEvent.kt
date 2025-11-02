package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import net.minecraft.client.gui.DrawContext
import at.hannibal2.skyhanni.utils.compat.SkyHanniGuiContainer
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot
import net.minecraft.item.ItemStack

abstract class GuiContainerEvent(open val gui: SkyHanniGuiContainer, open val container: ScreenHandler) : SkyHanniEvent() {

    data class BackgroundDrawnEvent(
        override val context: DrawContext,
        override val gui: SkyHanniGuiContainer,
        override val container: ScreenHandler,
        val mouseX: Int,
        val mouseY: Int,
        val partialTicks: Float,
    ) : GuiContainerEvent(gui, container), Rendering

    data class PreDraw(
        override val context: DrawContext,
        override val gui: SkyHanniGuiContainer,
        override val container: ScreenHandler,
        val mouseX: Int,
        val mouseY: Int,
        val partialTicks: Float,
    ) : GuiContainerEvent(gui, container), Cancellable, Rendering

    data class PostDraw(
        override val context: DrawContext,
        override val gui: SkyHanniGuiContainer,
        override val container: ScreenHandler,
        val mouseX: Int,
        val mouseY: Int,
        val partialTicks: Float,
    ) : GuiContainerEvent(gui, container), Rendering

    data class CloseWindowEvent(override val gui: SkyHanniGuiContainer, override val container: ScreenHandler) :
        GuiContainerEvent(gui, container), Cancellable

    abstract class DrawSlotEvent(gui: SkyHanniGuiContainer, container: ScreenHandler, open val slot: Slot) :
        GuiContainerEvent(gui, container) {

        data class GuiContainerDrawSlotPre(
            override val gui: SkyHanniGuiContainer,
            override val container: ScreenHandler,
            override val slot: Slot,
        ) :
            DrawSlotEvent(gui, container, slot), Cancellable

        data class GuiContainerDrawSlotPost(
            override val gui: SkyHanniGuiContainer,
            override val container: ScreenHandler,
            override val slot: Slot,
        ) :
            DrawSlotEvent(gui, container, slot)
    }

    data class ForegroundDrawnEvent(
        override val context: DrawContext,
        override val gui: SkyHanniGuiContainer,
        override val container: ScreenHandler,
        val mouseX: Int,
        val mouseY: Int,
        val partialTicks: Float,
    ) : GuiContainerEvent(gui, container), Rendering

    data class SlotClickEvent(
        override val gui: SkyHanniGuiContainer,
        override val container: ScreenHandler,
        val item: ItemStack?,
        val slot: Slot?,
        val slotId: Int,
        val clickedButton: Int,
        val clickType: ClickType?,
    ) : GuiContainerEvent(gui, container), Cancellable {

        fun makePickblock() {
            if (this.clickedButton == 2 && this.clickType == ClickType.MIDDLE) return
            slot?.id?.let { slotNumber ->
                InventoryUtils.clickSlot(slotNumber, container.syncId, mouseButton = 2, mode = 3)
                cancel()
            }
        }
    }

    enum class ClickType(val id: Int) {
        NORMAL(0),
        SHIFT(1),
        HOTBAR(2),
        MIDDLE(3),
        DROP(4),
        ;

        companion object {
            fun getTypeById(id: Int) = entries.firstOrNull { it.id == id }
        }
    }
}
