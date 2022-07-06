package at.lorenz.mod.events

import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.inventory.Container
import net.minecraft.inventory.ContainerChest
import net.minecraft.inventory.Slot
import net.minecraftforge.fml.common.eventhandler.Cancelable

abstract class GuiContainerEvent(open val gui: GuiContainer, open val container: Container) : LorenzEvent() {
    val chestName: String by lazy {
        if (container !is ContainerChest) error("Container is not a chest")
        return@lazy (container as ContainerChest).lowerChestInventory.displayName.unformattedText.trim()
    }

    data class BackgroundDrawnEvent(
        override val gui: GuiContainer,
        override val container: Container,
        val mouseX: Int,
        val mouseY: Int,
        val partialTicks: Float
    ) : GuiContainerEvent(gui, container)

    @Cancelable
    data class CloseWindowEvent(override val gui: GuiContainer, override val container: Container) :
        GuiContainerEvent(gui, container)

    abstract class DrawSlotEvent(gui: GuiContainer, container: Container, open val slot: Slot) :
        GuiContainerEvent(gui, container) {
        @Cancelable
        data class Pre(override val gui: GuiContainer, override val container: Container, override val slot: Slot) :
            DrawSlotEvent(gui, container, slot)

        data class Post(override val gui: GuiContainer, override val container: Container, override val slot: Slot) :
            DrawSlotEvent(gui, container, slot)
    }

    data class ForegroundDrawnEvent(
        override val gui: GuiContainer,
        override val container: Container,
        val mouseX: Int,
        val mouseY: Int,
        val partialTicks: Float
    ) : GuiContainerEvent(gui, container)

    @Cancelable
    data class SlotClickEvent(
        override val gui: GuiContainer,
        override val container: Container,
        val slot: Slot?,
        val slotId: Int,
        val clickedButton: Int,
        val clickType: Int
    ) : GuiContainerEvent(gui, container)
}