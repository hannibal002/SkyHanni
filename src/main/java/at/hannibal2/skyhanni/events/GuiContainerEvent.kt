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
        private var clickedButtonRaw: Int,
        var clickType: ContainerInput,
    ) : GuiContainerEvent(gui, container), Cancellable {

        val mouseType: MouseClickType get() = MouseClickType.of(clickedButtonRaw, clickType)
        fun rawButton(): Int = clickedButtonRaw

        // TODO remove in october 2026
        @Deprecated("use mouseType, or rawButton() when the raw value is needed")
        val clickedButton get() = clickedButtonRaw

        val slot: Slot?
            get() = slotId.takeIf { it > -1 }?.let(container::getSlot)

        val item: SafeItemStack?
            get() = slot?.item

        fun makePickblock() {
            if (clickType == CLONE) return
            if (slot == null) return

            clickedButtonRaw = 2
            clickType = CLONE
        }

        fun makeShiftClick() {
            if (mouseType.isRightClick() && slot?.item?.getItemCategoryOrNull() == SACK) return
            if (slot == null) return

            clickedButtonRaw = 0
            clickType = QUICK_MOVE
        }

        fun redirectClick(newSlotId: Int) {
            slotId = newSlotId
        }

        companion object {
            private val posting = ThreadLocal.withInitial { false }

            fun postEvent(
                gui: SkyHanniGuiContainer,
                container: AbstractContainerMenu,
                slotId: Int,
                clickedButton: Int,
                clickType: ContainerInput,
            ): SlotClickEvent? {
                if (posting.get()) return null

                posting.set(true)
                try {
                    val event = SlotClickEvent(gui, container, slotId, clickedButton, clickType)
                    event.post()
                    return event
                } finally {
                    posting.remove()
                }
            }
        }
    }
}

/**
 * The mouse button of a slot click, where it can be determined.
 *
 * Only PICKUP, QUICK_MOVE and CLONE carry a mouse button. For SWAP the raw value is the hotbar slot
 * index, for THROW it separates Q from Ctrl+Q, for QUICK_CRAFT it is a drag bitmask. Those are [OTHER].
 */
enum class MouseClickType {
    LEFT_CLICK,
    RIGHT_CLICK,
    MIDDLE_CLICK,
    OTHER,
    ;

    fun isLeftClick(): Boolean = this == LEFT_CLICK
    fun isRightClick(): Boolean = this == RIGHT_CLICK
    fun isMiddleClick(): Boolean = this == MIDDLE_CLICK

    /** The container input vanilla sends together with this button. */
    val defaultMode: ContainerInput get() = if (this == MIDDLE_CLICK) CLONE else PICKUP

    /** The raw button value to send. [OTHER] is sent as a left click. */
    val buttonId: Int
        get() = when (this) {
            LEFT_CLICK, OTHER -> 0
            RIGHT_CLICK -> 1
            MIDDLE_CLICK -> 2
        }

    companion object {
        fun of(button: Int, clickType: ContainerInput): MouseClickType = when (clickType) {
            PICKUP, QUICK_MOVE -> when (button) {
                0 -> LEFT_CLICK
                1 -> RIGHT_CLICK
                else -> OTHER
            }

            CLONE -> MIDDLE_CLICK
            else -> OTHER
        }
    }
}
