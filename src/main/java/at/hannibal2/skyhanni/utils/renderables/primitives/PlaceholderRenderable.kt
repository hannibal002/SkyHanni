package at.hannibal2.skyhanni.utils.renderables.primitives

import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.renderables.Renderable

fun Renderable.Companion.placeholder(width: Int, height: Int = 10) = object : Renderable {
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

fun Renderable.Companion.empty() = empty
fun Renderable.Companion.emptyText() = emptyText
