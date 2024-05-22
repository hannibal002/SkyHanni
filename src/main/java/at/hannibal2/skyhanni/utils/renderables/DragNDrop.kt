package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object DragNDrop {

    private var currentDrag: DragItem<*>? = null

    private var isInvalidDrop = false

    private val button = 0

    private val buttonMapped = button - 100

    @SubscribeEvent
    fun onGuiContainerBeforeDraw(event: GuiContainerEvent.BeforeDraw) {
        isInvalidDrop = false
    }

    @SubscribeEvent
    fun onGuiContainerAfterDraw(event: GuiContainerEvent.AfterDraw) {
        val item = currentDrag ?: return
        if (!buttonMapped.isKeyHeld()) {
            currentDrag = null
            return
        }
        GlStateManager.translate(event.mouseX.toFloat(), event.mouseY.toFloat(), 0f)
        item.onRender(event.mouseX, event.mouseY)
        GlStateManager.translate(-event.mouseX.toFloat(), -event.mouseY.toFloat(), 0f)
    }

    fun dragAble(
        display: Renderable,
        item: () -> DragItem<*>,
        bypassChecks: Boolean = false,
        condition: () -> Boolean = { true },
    ) = Renderable.clickable(
        display,
        onClick = { currentDrag = item() },
        button = button,
        bypassChecks = bypassChecks,
        condition = condition,
    )

    fun dropAble(
        display: Renderable,
        drop: DropAble,
        bypassChecks: Boolean = false,
        condition: () -> Boolean = { true },
    ): Renderable = object : RenderableWrapper(display) {
        override fun render(posX: Int, posY: Int) {
            if (isHovered(posX, posY) && condition() && Renderable.shouldAllowLink(true, bypassChecks)) {
                handelDropAble(drop)
            }
            content.render(posX, posY)
        }
    }

    private fun handelDropAble(drop: DropAble) {
        val item = currentDrag ?: return
        if (drop.validTarget(item.get())) {
            if (!buttonMapped.isKeyHeld()) {
                drop.handle(item.get())
                currentDrag = null
            }
        } else {
            isInvalidDrop = true
        }

    }
}

fun ItemStack.toDragItem(scale: Double = 1.0) = object : DragItem<ItemStack> {

    val render = Renderable.itemStack(this@toDragItem, scale, 0)

    override fun get(): ItemStack = this@toDragItem

    override fun onRender(mouseX: Int, mouseY: Int) = render.render(mouseX, mouseY)
}

interface DragItem<T> {

    fun get(): T
    fun onRender(mouseX: Int, mouseY: Int)

}

interface DropAble {

    fun handle(drop: Any?)
    fun validTarget(item: Any?): Boolean
}
