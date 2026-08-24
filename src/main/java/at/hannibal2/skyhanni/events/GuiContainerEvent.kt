package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.compat.SkyHanniGuiContainer
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.Slot

abstract class GuiContainerEvent(
    open val gui: SkyHanniGuiContainer,
    open val container: AbstractContainerMenu,
) : SkyHanniEvent() {

    @PrimaryFunction("onBackgroundDrawn")
    class BackgroundDrawnEvent(
        override val context: GuiGraphicsExtractor,
        override val gui: SkyHanniGuiContainer,
        override val container: AbstractContainerMenu,
        val mouseX: Int,
        val mouseY: Int,
        val partialTicks: Float,
    ) : GuiContainerEvent(gui, container), Rendering

    class PreDraw(
        override val context: GuiGraphicsExtractor,
        override val gui: SkyHanniGuiContainer,
        override val container: AbstractContainerMenu,
        val mouseX: Int,
        val mouseY: Int,
        val partialTicks: Float,
    ) : GuiContainerEvent(gui, container), Cancellable, Rendering

    class PostDraw(
        override val context: GuiGraphicsExtractor,
        override val gui: SkyHanniGuiContainer,
        override val container: AbstractContainerMenu,
        val mouseX: Int,
        val mouseY: Int,
        val partialTicks: Float,
    ) : GuiContainerEvent(gui, container), Rendering

    class CloseWindowEvent(
        override val gui: SkyHanniGuiContainer,
        override val container: AbstractContainerMenu,
    ) : GuiContainerEvent(gui, container), Cancellable

    abstract class DrawSlotEvent(
        gui: SkyHanniGuiContainer,
        container: AbstractContainerMenu,
        open val slot: Slot,
    ) : GuiContainerEvent(gui, container) {

        class GuiContainerDrawSlotPre(
            override val gui: SkyHanniGuiContainer,
            override val container: AbstractContainerMenu,
            override val slot: Slot,
        ) : DrawSlotEvent(gui, container, slot), Cancellable

        class GuiContainerDrawSlotPost(
            override val gui: SkyHanniGuiContainer,
            override val container: AbstractContainerMenu,
            override val slot: Slot,
        ) : DrawSlotEvent(gui, container, slot)
    }

    class ForegroundDrawnEvent(
        override val context: GuiGraphicsExtractor,
        override val gui: SkyHanniGuiContainer,
        override val container: AbstractContainerMenu,
        val mouseX: Int,
        val mouseY: Int,
        val partialTicks: Float,
    ) : GuiContainerEvent(gui, container), Rendering

    class SlotClickEvent private constructor(
        override val gui: SkyHanniGuiContainer,
        override val container: AbstractContainerMenu,
        var slotId: Int,
        var clickedButton: Int,
        var clickType: ContainerInput,
    ) : GuiContainerEvent(gui, container), Cancellable {

        val slot: Slot?
            get() = slotId.takeIf { it > -1 }?.let(container::getSlot)

        val item: SafeItemStack?
            get() = slot?.item

        fun makePickblock() {
            if (clickedButton == 2 && clickType == CLONE) return
            if (slot == null) return

            clickedButton = 2
            clickType = CLONE
        }

        fun makeShiftClick() {
            if (clickedButton == 1 && slot?.item?.getItemCategoryOrNull() == SACK) return
            if (slot == null) return

            clickedButton = 0
            clickType = QUICK_MOVE
        }

        fun redirectClick(newSlotId: Int) {
            slotId = newSlotId
        }

        companion object {
            private val postDepth = ThreadLocal.withInitial { 0 }

            fun postEvent(
                gui: SkyHanniGuiContainer,
                container: AbstractContainerMenu,
                slotId: Int,
                clickedButton: Int,
                clickType: ContainerInput,
            ): SlotClickEvent? {
                if (postDepth.get() > 0) return null

                postDepth.set(postDepth.get() + 1)
                try {
                    val event = SlotClickEvent(gui, container, slotId, clickedButton, clickType)
                    event.post()
                    return event
                } finally {
                    val depth = postDepth.get() - 1
                    check(depth >= 0) {
                        postDepth.remove()
                        "SlotClickEvent postDepth underflow detected"
                    }
                    if (depth == 0) postDepth.remove() else postDepth.set(depth)
                }
            }
        }
    }
}
