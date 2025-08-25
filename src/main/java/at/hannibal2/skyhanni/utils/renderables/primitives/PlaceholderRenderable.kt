package at.hannibal2.skyhanni.utils.renderables.primitives

import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableContext

fun RenderableContext.placeholder(width: Int, height: Int = 10) = object : Renderable {
    override val width = width
    override val height = height
    override val horizontalAlign = HorizontalAlignment.LEFT
    override val verticalAlign = VerticalAlignment.TOP

    @Suppress("EmptyFunctionBlock")
    override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
    }
}

private val empty = Renderable.placeholder(0, 0)
private val emptyText = Renderable.placeholder(0, 10)

fun RenderableContext.empty() = empty
fun RenderableContext.emptyText() = emptyText
