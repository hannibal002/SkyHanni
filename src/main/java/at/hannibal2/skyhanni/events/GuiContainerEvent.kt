package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.utils.GuiRenderUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.inventory.Container
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.Cancelable

abstract class GuiContainerEvent(open val gui: GuiContainer, open val container: Container) : LorenzEvent() {

    data class BackgroundDrawnEvent(
        override val gui: GuiContainer,
        override val container: Container,
        val mouseX: Int,
        val mouseY: Int,
        val partialTicks: Float,
    ) : GuiContainerEvent(gui, container)

    @Cancelable
    data class BeforeDraw(
        override val gui: GuiContainer,
        override val container: Container,
        val mouseX: Int,
        val mouseY: Int,
        val partialTicks: Float,
    ) : GuiContainerEvent(gui, container) {
        fun drawDefaultBackground() =
            GuiRenderUtils.drawGradientRect(0, 0, gui.width, gui.height, -1072689136, -804253680, 0.0)
    }

    @Cancelable
    data class CloseWindowEvent(override val gui: GuiContainer, override val container: Container) :
        GuiContainerEvent(gui, container)

    abstract class DrawSlotEvent(gui: GuiContainer, container: Container, open val slot: Slot) :
        GuiContainerEvent(gui, container) {

        @Cancelable
        data class GuiContainerDrawSlotPre(
            override val gui: GuiContainer,
            override val container: Container,
            override val slot: Slot,
        ) :
            DrawSlotEvent(gui, container, slot)

        data class GuiContainerDrawSlotPost(
            override val gui: GuiContainer,
            override val container: Container,
            override val slot: Slot,
        ) :
            DrawSlotEvent(gui, container, slot)
    }

    data class ForegroundDrawnEvent(
        override val gui: GuiContainer,
        override val container: Container,
        val mouseX: Int,
        val mouseY: Int,
        val partialTicks: Float,
    ) : GuiContainerEvent(gui, container)

    @Cancelable
    data class SlotClickEvent(
        override val gui: GuiContainer,
        override val container: Container,
        val item: ItemStack?,
        val slot: Slot?,
        val slotId: Int,
        val clickedButton: Int,
        @Deprecated("old", ReplaceWith("clickTypeEnum"))
        val clickType: Int,
        val clickTypeEnum: ClickType? = ClickType.getTypeById(clickType),
    ) : GuiContainerEvent(gui, container) {

        fun makePickblock() {
            if (this.clickedButton == 2 && this.clickTypeEnum == ClickType.HOTBAR) return
            slot?.slotNumber?.let { slotNumber ->
                Minecraft.getMinecraft().playerController.windowClick(
                    container.windowId, slotNumber, 2, 3, Minecraft.getMinecraft().thePlayer
                )
                isCanceled = true
            }
        }
    }

    enum class ClickType(val id: Int) {
        NORMAL(1),
        SHIFT(2),
        HOTBAR(3),
        MIDDLE(4),
        DROP(5),
        ;

        companion object {
            fun getTypeById(id: Int) = entries.firstOrNull { it.id == id }
        }
    }
}
