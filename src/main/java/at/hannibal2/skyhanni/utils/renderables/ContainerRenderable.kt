package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.renderables.Renderable.Companion.filterList
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXAligned
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderYAligned
import net.minecraft.client.renderer.GlStateManager

open class RenderableContainer(
    var renderables: Collection<Renderable>,
    spacing: Int = 0,
    override val horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
    override val verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
    private val onRender: (Int, Int, Int, Int, Collection<Renderable>) -> Unit = { _, _, _, _, _ -> },
) : Renderable {
    override val width = renderables.sumOf { it.width } + spacing * (renderables.size - 1)
    override val height = renderables.maxOfOrNull { it.height } ?: 0

    override fun render(posX: Int, posY: Int) = onRender.invoke(posX, posY, width, height, renderables)
}

open class VerticalContainerRenderable(
    containerContent: Collection<Renderable>,
    spacing: Int = 0,
    override val horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
    override val verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
) : Renderable, RenderableContainer(
    containerContent,
    spacing,
    horizontalAlign,
    verticalAlign,
    onRender = { posX, posY, width, height, renderables ->
        var yOffset = posY
        renderables.forEach {
            it.renderXAligned(posX, yOffset, width)
            yOffset += it.height + spacing
            GlStateManager.translate(0f, (it.height + spacing).toFloat(), 0f)
        }
        GlStateManager.translate(0f, -height.toFloat() - spacing.toFloat(), 0f)
    }
)

class HorizontalRenderableContainer(
    containerContent: Collection<Renderable>,
    spacing: Int = 0,
    override val horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
    override val verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
) : Renderable, RenderableContainer(
    containerContent,
    spacing,
    horizontalAlign,
    verticalAlign,
    onRender = { posX, posY, width, height, renderables ->
        var xOffset = posX
        renderables.forEach {
            it.renderYAligned(xOffset, posY, height)
            xOffset += it.width + spacing
            GlStateManager.translate((it.width + spacing).toFloat(), 0f, 0f)
        }
        GlStateManager.translate(-width.toFloat() - spacing.toFloat(), 0f, 0f)
    }
)

class SearchableVerticalContainer(
    private val content: Map<Renderable, String?>,
    spacing: Int = 0,
    private val textInput: TextInput,
    private val key: Int,
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
