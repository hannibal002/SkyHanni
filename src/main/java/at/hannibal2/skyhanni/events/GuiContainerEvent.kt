package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.compat.SkyHanniGuiContainer
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

abstract class GuiContainerEvent(open val gui: SkyHanniGuiContainer, open val container: AbstractContainerMenu) : SkyHanniEvent() {

    data class BackgroundDrawnEvent(
        override val context: GuiGraphics,
        override val gui: SkyHanniGuiContainer,
        override val container: AbstractContainerMenu,
        val mouseX: Int,
        val mouseY: Int,
        val partialTicks: Float,
    ) : GuiContainerEvent(gui, container), Rendering

    data class PreDraw(
        override val context: GuiGraphics,
        override val gui: SkyHanniGuiContainer,
        override val container: AbstractContainerMenu,
        val mouseX: Int,
        val mouseY: Int,
        val partialTicks: Float,
    ) : GuiContainerEvent(gui, container), Cancellable, Rendering

    data class PostDraw(
        override val context: GuiGraphics,
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
        override val context: GuiGraphics,
        override val gui: SkyHanniGuiContainer,
        override val container: AbstractContainerMenu,
        val mouseX: Int,
        val mouseY: Int,
        val partialTicks: Float,
    ) : GuiContainerEvent(gui, container), Rendering

    data class SlotClickEvent(
        override val gui: SkyHanniGuiContainer,
        override val container: AbstractContainerMenu,
        val item: ItemStack?,
        val slot: Slot?,
        val slotId: Int,
        val clickedButton: Int,
        val clickType: ClickType?,
    ) : GuiContainerEvent(gui, container), Cancellable {

        fun makePickblock() {
            if (this.clickedButton == 2 && this.clickType == ClickType.MIDDLE) return
            slot?.index?.let { slotNumber ->
                InventoryUtils.clickSlot(slotNumber, container.containerId, mouseButton = 2, mode = ClickType.MIDDLE)
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
