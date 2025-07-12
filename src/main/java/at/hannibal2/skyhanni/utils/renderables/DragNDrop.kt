package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.renderables.item.ItemStackRenderable
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack

@SkyHanniModule
object DragNDrop {

    private var currentDrag: DragItem<*>? = null

    private var isInvalidDrop = false

    private const val BUTTON_MAPPED = -100

    private val invalidItem = ItemStackRenderable(ItemStack(Blocks.barrier), 1.0)

    @HandleEvent
    fun onGuiContainerBeforeDraw(event: GuiContainerEvent.PreDraw) {
        isInvalidDrop = false
    }

    @HandleEvent
    fun onGuiContainerAfterDraw(event: GuiContainerEvent.PostDraw) {
        val item = currentDrag ?: return
        if (!BUTTON_MAPPED.isKeyHeld()) {
            currentDrag = null
            return
        }
        DrawContextUtils.translate(event.mouseX.toFloat(), event.mouseY.toFloat(), 0f)
        if (isInvalidDrop) {
            invalidItem.render(event.mouseX, event.mouseY)
        } else {
            item.onRender(event.mouseX, event.mouseY)
        }
        DrawContextUtils.translate(-event.mouseX.toFloat(), -event.mouseY.toFloat(), 0f)
    }

    fun draggable(
        display: Renderable,
        item: () -> DragItem<*>,
        bypassChecks: Boolean = false,
        condition: () -> Boolean = { true },
    ) = Renderable.clickable(
        display,
        onLeftClick = { currentDrag = item() },
        bypassChecks = bypassChecks,
        condition = condition,
    )

    fun droppable(
        display: Renderable,
        drop: Droppable,
        bypassChecks: Boolean = false,
        condition: () -> Boolean = { true },
    ): Renderable = object : RenderableWrapper(display) {
        override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
            if (isHovered(mouseOffsetX, mouseOffsetY) && condition() && Renderable.shouldAllowLink(true, bypassChecks)) {
                handelDroppable(drop)
            }
            content.render(mouseOffsetX, mouseOffsetY)
        }
    }

    private fun handelDroppable(drop: Droppable) {
        val item = currentDrag ?: return
        if (drop.validTarget(item.get())) {
            if (!BUTTON_MAPPED.isKeyHeld()) {
                drop.handle(item.get())
                currentDrag = null
            }
        } else {
            isInvalidDrop = true
        }

    }
}

fun ItemStack.toDragItem(scale: Double = 1.0) = object : DragItem<ItemStack> {

    val render = ItemStackRenderable(this@toDragItem, scale, 0)

    override fun get(): ItemStack = this@toDragItem

    override fun onRender(mouseX: Int, mouseY: Int) = render.render(mouseX, mouseY)
}

interface DragItem<T> {

    fun get(): T
    fun onRender(mouseX: Int, mouseY: Int)

}

interface Droppable {

    fun handle(drop: Any?)
    fun validTarget(item: Any?): Boolean
}
