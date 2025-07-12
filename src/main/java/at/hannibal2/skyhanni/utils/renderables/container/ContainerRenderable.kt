package at.hannibal2.skyhanni.utils.renderables.container

import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Renderable.Companion.filterList
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXAligned
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderYAligned

abstract class ContainerRenderable(
    var renderables: Collection<Renderable>,
    open val spacing: Int = 0,
    override val horizontalAlign: HorizontalAlignment,
    override val verticalAlign: VerticalAlignment
) : Renderable {
    abstract override val width: Int
    abstract override val height: Int
    abstract override fun render(mouseOffsetX: Int, mouseOffsetY: Int)
}

open class VerticalContainerRenderable(
    renderables: Collection<Renderable>,
    spacing: Int = 0,
    horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
    verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
) : ContainerRenderable(renderables, spacing, horizontalAlign, verticalAlign) {

    override val width = renderables.maxOfOrNull { it.width } ?: 0

    override val height = renderables.sumOf { it.height } + spacing * (renderables.size - 1)

    override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
        var y = mouseOffsetY
        renderables.forEach {
            it.renderXAligned(mouseOffsetX, y, width)
            y += it.height + spacing
            DrawContextUtils.translate(0f, (it.height + spacing).toFloat(), 0f)
        }
        DrawContextUtils.translate(0f, (-height - spacing).toFloat(), 0f)
    }
}

class HorizontalContainerRenderable(
    renderables: Collection<Renderable>,
    spacing: Int = 0,
    horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
    verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
) : ContainerRenderable(renderables, spacing, horizontalAlign, verticalAlign) {

    override val width = renderables.sumOf { it.width } + spacing * (renderables.size - 1)

    override val height = renderables.maxOfOrNull { it.height } ?: 0

    override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
        var x = mouseOffsetX
        renderables.forEach {
            it.renderYAligned(x, mouseOffsetY, height)
            x += it.width + spacing
            DrawContextUtils.translate((it.width + spacing).toFloat(), 0f, 0f)
        }
        DrawContextUtils.translate((-width - spacing).toFloat(), 0f, 0f)
    }
}

class SearchableVerticalContainer(
    private val content: Map<Renderable, String?>,
    spacing: Int = 0,
    private val textInput: TextInput,
    key: Int,
    horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
    verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
) : VerticalContainerRenderable(
    content.map { it.key },
    spacing,
    horizontalAlign,
    verticalAlign,
) {
    init {
        textInput.registerToEvent(key) {
            // null = ignored, never filtered
            renderables = filterList(content, textInput.textBox)
        }
    }
}
