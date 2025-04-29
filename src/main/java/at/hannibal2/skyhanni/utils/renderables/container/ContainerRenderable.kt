package at.hannibal2.skyhanni.utils.renderables.container

import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
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
    abstract override fun render(posX: Int, posY: Int)
}

open class VerticalContainerRenderable(
    renderables: Collection<Renderable>,
    spacing: Int = 0,
    horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
    verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
) : Renderable, ContainerRenderable(renderables, spacing, horizontalAlign, verticalAlign) {

    override val width: Int
        get() = renderables.maxOfOrNull { it.width } ?: 0

    override val height: Int
        get() = renderables.sumOf { it.height } + spacing * (renderables.size - 1)

    override fun render(posX: Int, posY: Int) {
        var y = posY
        renderables.forEach {
            it.renderXAligned(posX, y, width)
            y += it.height + spacing
        }
    }
}

class HorizontalContainerRenderable(
    renderables: Collection<Renderable>,
    spacing: Int = 0,
    horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
    verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
) : Renderable, ContainerRenderable(renderables, spacing, horizontalAlign, verticalAlign) {

    override val width: Int
        get() = renderables.sumOf { it.width } + spacing * (renderables.size - 1)

    override val height: Int
        get() = renderables.maxOfOrNull { it.height } ?: 0

    override fun render(posX: Int, posY: Int) {
        var x = posX
        renderables.forEach {
            it.renderYAligned(x, posY, height)
            x += it.width + spacing
        }
    }
}

class SearchableVerticalContainer(
    private val content: Map<Renderable, String?>,
    spacing: Int = 0,
    private val textInput: TextInput,
    key: Int,
    horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
    verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
) : Renderable, VerticalContainerRenderable(
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
