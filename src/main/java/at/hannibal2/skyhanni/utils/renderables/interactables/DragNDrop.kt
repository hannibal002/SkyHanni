package at.hannibal2.skyhanni.utils.renderables.interactables

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyClicked
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.decorators.RenderableDecoratorOnlyRender
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack

@SkyHanniModule
object DragNDrop {

    private var currentDrag: DragItem<*>? = null

    private var isInvalidDrop = false

    private const val BUTTON_MAPPED = KeyboardManager.LEFT_MOUSE

    private val invalidItem = Renderable.item(ItemStack(Blocks.barrier), 1.0)

    @HandleEvent
    fun onGuiContainerBeforeDraw(event: GuiContainerEvent.PreDraw) {
        isInvalidDrop = false
    }

    @HandleEvent
    fun onGuiContainerAfterDraw(event: GuiContainerEvent.PostDraw) {
        val item = currentDrag ?: return
        if (!BUTTON_MAPPED.isKeyHeld()) {
            currentDrag?.onDiscard()
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

    fun Renderable.Companion.draggable(
        display: Renderable,
        item: () -> DragItem<*>,
        bypassChecks: Boolean = false,
        condition: () -> Boolean = { true },
    ) = clickable(
        display,
        onLeftClick = { currentDrag = item() },
        bypassChecks = bypassChecks,
        condition = condition,
    )

    fun Renderable.Companion.droppable(
        display: Renderable,
        drop: Droppable,
        bypassChecks: Boolean = false,
        condition: () -> Boolean = { true },
    ): Renderable = object : RenderableDecoratorOnlyRender {
        override val root = display
        override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
            if (isHovered(mouseOffsetX, mouseOffsetY) && condition() && shouldAllowLink(true, bypassChecks)) {
                handelDroppable(drop)
            }
            root.render(mouseOffsetX, mouseOffsetY)
        }
    }

    fun handelDroppable(drop: Droppable) {
        val item = currentDrag?.get() ?: return
        if (drop.validTarget(item)) {
            if (!BUTTON_MAPPED.isKeyHeld()) {
                drop.handle(item)
                currentDrag = null
            }else{
                drop.preDrop(item)
            }
        } else {
            isInvalidDrop = true
        }
    }

    fun dragOnPress(item: DragItem<*>, onClick: () -> Unit) {
        if (BUTTON_MAPPED.isKeyClicked()) {
            currentDrag = item
            onClick()
        }
    }
}

fun ItemStack.toDragItem(scale: Double = 1.0) = object : DragItem<ItemStack> {

    val render = Renderable.item(this@toDragItem, scale, 0)

    override fun get(): ItemStack = this@toDragItem

    override fun onRender(mouseX: Int, mouseY: Int) = render.render(mouseX, mouseY)
}

interface DragItem<T> {

    fun get(): T
    fun onRender(mouseX: Int, mouseY: Int)
    fun onDiscard(){}
}

interface Droppable {

    fun handle(drop: Any?)
    fun preDrop(drop: Any?){}
    fun validTarget(item: Any?): Boolean
}
